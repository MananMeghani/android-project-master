package com.example.appforblind;

import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.LocationListener;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    String phoneNumber = "";
    ArrayList<ContentProviderOperation> ops = null;
    private TextView txtSpeechInput;
    private ImageButton btnSpeak;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    static String[] msgArr = new String[5];
    static int no = -5;
    static int text = 0;
    static int contacts = 0;
    private TextToSpeech tts;

    static String name = "", number = "";
    static String saveContact[] = new String[15];
    String msgContent[] = {"i am busy", "not feeling well", "talk to you later", "come home right now", "lost my wallet"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {  // initailze audio and content provider
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tts = new TextToSpeech(this, this);
        txtSpeechInput = (TextView) findViewById(R.id.txtSpeechInput);

        ops = new ArrayList<ContentProviderOperation>();

        ops.add(ContentProviderOperation.newInsert(
                        ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                        .build()
        );

        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int amStreamMusicMaxVol = am.getStreamMaxVolume(am.STREAM_MUSIC);
        am.setStreamVolume(am.STREAM_MUSIC, amStreamMusicMaxVol, 0);


    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {  // volume key press events
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {

                    promptSpeechInput();
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {

                    promptSpeechInput();
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    /**
     * Showing google speech input dialog
     */
    private void promptSpeechInput() { //open  goggle speech input
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Receiving speech input
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) { //get data and process from what user spoke
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    txtSpeechInput.setText(result.get(0));

                    tts.speak(result.get(0), TextToSpeech.QUEUE_FLUSH, null);


                    if (result.get(0).toString().contains("add contact")) {
                        contacts = 2;
                        Log.i("Contacts ", "value" + contacts);
                        Toast.makeText(this, "Command " + result.get(0).toString(), Toast.LENGTH_SHORT).show();

                    }
                    if (contacts == 2 && result.get(0).toString().contains("name")) {

                        name = result.get(0).toString().substring(5);
                        contacts = 3;
                        Log.i("Contacts ", "name :" + name + "value" + contacts);
                        Toast.makeText(this, "Command " + name, Toast.LENGTH_SHORT).show();
                    }
                    if (contacts == 3 && name != " " && result.get(0).toString().contains("number")) {

                        number = result.get(0).toString().substring(7);
                        number = number.trim();
                        contacts = 4;
                        if (number.length() < 10) {
                            contacts = 3;
                        }
                        Log.i("Contacts ", "number :" + number.trim() + "value" + contacts);
                        Toast.makeText(this, "Command " + number, Toast.LENGTH_SHORT).show();
                    }
                    if (contacts == 4 && result.get(0).toString().contains("save contact")) {
                        try {
                            if (name != null) {
                                System.out.println("name" + name);
                                ops.add(ContentProviderOperation.newInsert(
                                                ContactsContract.Data.CONTENT_URI)
                                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                                .withValue(ContactsContract.Data.MIMETYPE,
                                                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                                                .withValue(
                                                        ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                                                        name).build()
                                );
                            }

                            //------------------------------------------------------ Mobile Number
                            if (number != null) {
                                System.out.println("number" + number);
                                ops.add(ContentProviderOperation.
                                                newInsert(ContactsContract.Data.CONTENT_URI)
                                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                                .withValue(ContactsContract.Data.MIMETYPE,
                                                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                                                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                                                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                                                .build()
                                );
                            }

                            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                            Toast.makeText(this, "Contact added", Toast.LENGTH_SHORT).show();
                            contacts = 0;
                            name = " ";
                            number = " ";
                            Log.i("Contacts ", "value" + contacts);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (result.get(0).toString().contains("call")) {
                        name = result.get(0).toString().substring(5);

                        Intent intent = new Intent(Intent.ACTION_CALL);
                        intent.setData(Uri.parse("tel:" + makeCall(name)));
                        Log.i("Calling ", "name " + name + " number : " + makeCall(name));
                        startActivity(intent);
                        name = "";

                    }

                    if (result.get(0).toString().contains("message")) {
                        // msgArr=result.get(0).toString().split(" ");
                        name = result.get(0).toString().substring(8);

                        text = 1;
                        Toast.makeText(this, "text " + name + "=========>" + text, Toast.LENGTH_SHORT).show();
                    }

                    if (text == 1 && result.get(0).toString().contains("content")) {
                        number = result.get(0).toString().substring(8);
                        //for(int i=0;i<5;i++){
                        //	if(msgContent[i].contains(number)){
                        //msgArr[2]=String.valueOf(i);
                        //		no=i;
                        Toast.makeText(this, "content " + number + "=NAME========>" + name, Toast.LENGTH_SHORT).show();
                        //	}
                        //}
                    }
                    if (result.get(0).toString().contains("send") && number != " ") {
                        SmsManager sms = SmsManager.getDefault();
                        System.out.println("name" + name);
                        makeCall(name);
                        Toast.makeText(getApplicationContext(),
                                "Name : " + name + " Number : " + phoneNumber + " Content -> : " + number,
                                Toast.LENGTH_LONG).show();
                        System.out.println(phoneNumber);
                        sms.sendTextMessage(makeCall(name), null, number, null, null);

                        name = " ";
                        number = " ";
                        text = 0;
                        //no=0;
                    }

                }
                break;
            }

        }
    }

    public void testing() {
        String response = makeCall("Agam Mandir");
        if (response.toLowerCase().contains("unable") || response.toLowerCase().contains("contacts with same name")) {
            //pass response to tts
            tts.speak(response, TextToSpeech.QUEUE_FLUSH, null);
        } else {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + makeCall(response)));
            startActivity(intent);
        }


    }

    // manan blind Application

    public String makeCall(String str) {

        ArrayList<Person> personArrayList = new ArrayList<Person>();
        //return phone number from contacts
        Cursor cursor = null;
        try {
            cursor = getApplicationContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int phoneNumberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            cursor.moveToFirst();
            do {
                String name = cursor.getString(nameIdx);
                String phoneNumber = cursor.getString(phoneNumberIdx);
                if (name.toLowerCase().contains(str.toLowerCase())) {

                    personArrayList.add(new Person(name, phoneNumber));

                }


                //...
            } while (cursor.moveToNext());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }

        }

        if (personArrayList.size() == 0) {
            return "unable to find " + str;
        } else if (personArrayList.size() == 1) {
            return personArrayList.get(0).getNumber();
        } else {
            return "There are " + (personArrayList.size() - 1) + " contacts with same name";
        }


    }

    class Person {
        String name;
        String number;

        public Person(String name, String number) {
            this.name = name;
            this.number = number;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }


        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
        }

    }
        public void onInit(int status) {
            // TODO Auto-generated method stub
            if (status == TextToSpeech.SUCCESS) {

                int result = tts.setLanguage(Locale.US);

                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "This Language is not supported");
                } else {

                    tts.speak("Welcome to Blind Application. please press the volume key.", TextToSpeech.QUEUE_FLUSH, null);
                }

            } else {
                Log.e("TTS", "Initialization Failed!");
            }
        }

        @Override
        public void onDestroy() {
            // Don't forget to shutdown tts!
            if (tts != null) {
                tts.stop();
                tts.shutdown();
            }
            super.onDestroy();
        }

}
