/**
 * 
 */
package home.app.heart;

import android.app.Activity;
import android.os.Bundle;

/**
 * @author Norbert
 *
 */
public class Connected extends Activity {
	
	GraphView graphView;
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	
        graphView = new GraphView(this,"ECG signal", 20);
		setContentView(graphView);
        //setContentView(R.layout.btconnected);
        //LinearLayout layout = (LinearLayout) findViewById(R.id.chart);

        //layout.addView(SingletonGraph.instance().createGraph(getApplicationContext()));
    }
    
    protected void onStart(){
    	super.onStart();
    	graphView.run();
    }
    
    protected void onRestart(){
    	super.onRestart();
    	graphView.run();
    }

    protected void onResume(){
    	super.onResume();
    	graphView.run();
    }

    protected void onPause(){
    	super.onPause();
    }

    protected void onStop(){
    	super.onStop();
    }
    
    public void onDestroy(){
    	super.onDestroy();
    }
    
}
