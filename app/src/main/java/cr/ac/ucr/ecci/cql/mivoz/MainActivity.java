package cr.ac.ucr.ecci.cql.mivoz;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;

import com.ibm.watson.developer_cloud.http.ServiceCall;
import com.ibm.watson.developer_cloud.language_translator.v2.LanguageTranslator;
import com.ibm.watson.developer_cloud.language_translator.v2.model.Language;
import com.ibm.watson.developer_cloud.language_translator.v2.model.TranslationResult;

public class MainActivity extends AppCompatActivity {
    // Codigo de retorno del intent
    private static final int REQUEST_CODE = 1;
    // Lenguaje
    private String mLenguaje = "en-US";
    private String mLenguajeDestino = "es-ES";
    private Locale mLocale = Locale.US;
    // Texto y botones iniciales de la actividad principal
    TextView mTexto;
    // Opcion de Voz a texto
    Button mSpeechText;
    // Lista de textos encontrados
    ArrayList<String> mMatchesText;
    // Lista para mostrar los textos
    ListView mTextListView ;
    // Dialogo para mostrar la lista
    Dialog mMatchTextDialog;
    // Instancia de Texto a voz
    TextToSpeech mTextToSpeech;
    // Texto de entrada
    EditText mEditText;
    // Opcion de Texto a voz
    Button mTextSpeech;
    //Boton para traducir el texto
    Button mTranslate;
    //resultado de la traduccion
    TextView mTranslationResult;
    //Barra progreso para proceso de traduccion
    ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // componentes de la aplicacion
        mSpeechText = (Button) findViewById(R.id.buttonSpeechText);
        mTextSpeech = (Button) findViewById(R.id.buttonTextSpeech);
        mTranslate = (Button) findViewById(R.id.buttonTranslate);
        mTexto = (TextView) findViewById(R.id.texto);
        mEditText = (EditText) findViewById(R.id.editText);
        mTranslationResult = (TextView) findViewById(R.id.translationResult);
        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Traduciendo texto...");
        // intent para el reconocimiento de voz
        mSpeechText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isConnected ()){
                     // intent al API de reconocimiento de voz
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE , mLenguaje);
                    startActivityForResult(intent, REQUEST_CODE);
                } else {
                    Toast.makeText(getApplicationContext(), "No hay conexión a Internet",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        // Set el lenguaje de texto a voz
        mTextToSpeech = new TextToSpeech(getApplicationContext(), new
                TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit (int status) {
                        mTextToSpeech.setLanguage(mLocale);
                    }
                });
        // Intent para texto a voz
        mTextSpeech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                // lenguaje
                mTextToSpeech.setLanguage(mLocale);
                // Texto el edit
                String toSpeak = mEditText.getText().toString();
                Toast.makeText(getApplicationContext(), toSpeak, Toast.LENGTH_SHORT).show();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                    // Mayor que Lollipop
                    mTextToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }
        });

        mTranslate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String toTranslate = mEditText.getText().toString();
                if(isConnected()) {
                    if (!toTranslate.isEmpty() && toTranslate != null) {
                        new TranslationTask().execute(toTranslate);
                    } else {
                        Toast.makeText(getApplicationContext(), "Ingrese un texto a traducir", Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(), "No hay conexión a Internet", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // evento de seleccion de idioma
    public void onRadioButtonClicked(View view) {
        // Verificar el RadioButton seleccionado
        boolean checked = ((RadioButton) view).isChecked();
        // Determinar cual RadioButton esta seleccionado
        switch(view.getId()) {
            case R.id.radioButtonIngles:
                if (checked) {
                    mLenguaje = "en-US";
                    mLenguajeDestino = "es-ES";
                }
                mLocale = Locale.US;
                break;
            case R.id.radioButtonEspannol:
                if (checked) {
                    mLenguaje = "es-ES";
                    mLenguajeDestino = "en-US";
                }
                mLocale = new Locale("spa", "ESP");
                break;
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Valores de retorno del intent
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK ) {
            // Si retorna resultados el servicion de reconocimiento de voz
            // Creamos el dialogo y asignamos la lista
            mMatchTextDialog = new Dialog(MainActivity.this);
            mMatchTextDialog.setContentView(R.layout.dialog_matches_frag);
            // título del dialogo
            mMatchTextDialog.setTitle("Seleccione el texto");
            // Lista de elementos
            mTextListView = (ListView) mMatchTextDialog.findViewById(R.id.listView1);
            mMatchesText = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            // Mostramos los datos en la lista
            ArrayAdapter<String> adapter = new ArrayAdapter <String>(this,
                    android.R.layout.simple_list_item_1, mMatchesText);
            mTextListView.setAdapter(adapter);
            // Asignamos el evento del clic en la lista
            mTextListView.setOnItemClickListener(new AdapterView.OnItemClickListener () {
                @Override
                public void onItemClick(AdapterView <?> parent, View view, int position, long
                        id) {
                    mTexto.setText("You have said: " + mMatchesText.get(position));
                    mEditText.setText(mMatchesText.get(position));
                    mMatchTextDialog.hide();
                }});
            mMatchTextDialog.show();
        }
    }
    // verificar conexion a internet
    public boolean isConnected (){
        ConnectivityManager cm = (ConnectivityManager) getSystemService
                (Context.CONNECTIVITY_SERVICE);
        NetworkInfo net = cm.getActiveNetworkInfo();
        if(net != null && net.isAvailable() && net.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    private class TranslationTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            mProgress.show();
        }

        @Override
        protected String doInBackground(String... text) {
            // tomamos el parámetro del execute() y traducimos
            return translateText(text[0], mLenguaje, mLenguajeDestino);
        }

        // Se muestra el resultado de la traduccion
        protected void onPostExecute(String result) {
            mTranslationResult.setText(result);
            //se oculta el teclado para mostrar el resultado
            hideSoftKeyBoard();
            mProgress.hide();
        }
    }

    public String translateText(String text, String source, String target){
        //IBM utiliza identificadores de idiomas distintos de los de google, hay que mapearlos
        LanguageTranslator service = new LanguageTranslator();
        //se asignan credenciales para conectarse al servicio de IBM
        service.setUsernameAndPassword("2738a442-5b07-4e64-a2c5-125e577695a1","7wxVv7fY0Bfr");
        Language from;
        Language to;
        //simplificado para solo los casos ingles-espanol y viceversa
        from = (source.contains("en") ? Language.ENGLISH :  Language.SPANISH) ;
        to = (target.contains("en") ? Language.ENGLISH :  Language.SPANISH) ;
        ServiceCall<TranslationResult> serviceCall = service.translate( text, from, to);
        TranslationResult result = serviceCall.execute();
        return result.getFirstTranslation();
    }

    //Metodo para ocultar teclado
    //@author pier-luc-gendreau
    private void hideSoftKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        if(imm.isAcceptingText()) { // verify if the soft keyboard is open
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }
}
