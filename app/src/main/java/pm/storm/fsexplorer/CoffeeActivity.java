package pm.storm.fsexplorer;

import android.app.AlertDialog;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

/**
 * Created by michael on 3/1/15.
 */
public class CoffeeActivity extends ActionBarActivity {
    private Firestorm tgt;
    private BluetoothGatt gatt;
    UUID svc;
    UUID attr;
    String sSvc;
    String sAttr;
    ManifestResolver.ManifestFormatEntry [] fields;
    View[] fieldUI;
    int coffeeAmount = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coffee);
        Intent intent = getIntent();
        tgt = (Firestorm) intent.getParcelableExtra("firestorm");
        gatt = tgt.getDevice().connectGatt(this, false, gatt_cb);
        svc = ((ParcelUuid)intent.getParcelableExtra("svc")).getUuid();
        attr = ((ParcelUuid)intent.getParcelableExtra("attr")).getUuid();
        sSvc = svc.toString().substring(4,8);
        sAttr = attr.toString().substring(4,8);
        //Build the display
        genfields();
    }

    public void genfields() {
        fields = ManifestResolver.getInstance(this).getFields(sSvc, sAttr);
        fieldUI = new View[fields.length];
//        System.out.println("MFARR "+fields+" len "+fields.length);
        for (int i = 0; i < fields.length; i++) {
            fieldUI[i] = addField(fields[i].name, fields[i].desc, fields[i].getTypeAsString(), fields[i].isNumeric());
        }
    }
    public void onBackPressed()
    {
        gatt.disconnect();
        super.onBackPressed();
    }

    private View addField(String name, String desc, String type, boolean isNumeric) {
        View fv;
        LayoutInflater inflater = (LayoutInflater) CoffeeActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        fv = inflater.inflate(R.layout.cof_field, null);
        TextView text = ((TextView) fv.findViewById(R.id.fieldName));
        text.setText(name);
        if (name.equals("secH20")) {
            text.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
//                    Toast.makeText(getBaseContext(), "click secH20", Toast.LENGTH_SHORT).show();
                    CoffeeActivity.this.showDialog();
                }
            });
        }
        TextView field = ((TextView) fv.findViewById(R.id.fieldDesc));
        field.setText(desc);

        LinearLayout layout = (LinearLayout) findViewById(R.id.cofFieldList);
        fv.setId(0);
        fv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(fv);
        return fv;
    }



    BluetoothGattCallback gatt_cb = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            System.out.println("Connection changed");
            gatt.discoverServices();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_attribute, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void wbOnClick(View v) {
        byte [] chardata = new byte[20];
        chardata[0] = (byte) 0x80;
        chardata[1] = (byte) 0x90;
        for (ManifestResolver.ManifestFormatEntry mfe : fields) {
            System.out.println(mfe);
        }
        if (send(chardata)) {
            Toast.makeText(getBaseContext(), "Test Success", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getBaseContext(), "Failed to Write", Toast.LENGTH_LONG).show();
        }
        //Get the values of all the fields
    }

    public void showDialog()
    {
        final AlertDialog.Builder popDialog = new AlertDialog.Builder(this);
        final SeekBar seek = new SeekBar(this);
        seek.setMax(8000);

//        popDialog.setIcon(android.R.drawable.btn_star_big_on);
        popDialog.setTitle("How much coffee? (0-2 cups)");
        popDialog.setView(seek);

        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                //Do something here with new value
                CoffeeActivity.this.coffeeAmount = progress;
            }

            public void onStartTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub

            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }
        });


        // Button OK
        popDialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
//                        Toast.makeText(getBaseContext(), "Trying command", Toast.LENGTH_SHORT).show()
                        CoffeeActivity.this.sendMakeCoffee();
                        dialog.dismiss();
                    }

                });
        popDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });


        popDialog.create();
        popDialog.show();

    }

    protected void sendMakeCoffee() {
        byte[] data = new byte[2];
        data[0] = (byte) (coffeeAmount & 0xff);
        data[1] = (byte) (coffeeAmount >> 8);
        double cups = ((double) coffeeAmount) * (2.0/8000.0);
        String shortCups = Double.toString(cups).substring(0,4);
        if (send(data)) {
            Toast.makeText(getBaseContext(), "Making " + shortCups + " cups of coffee!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getBaseContext(), "Can't find coffee machine", Toast.LENGTH_LONG).show();
        }
    }

    protected boolean send(byte[] data) {
        BluetoothGattService bgs = gatt.getService(svc);
        if (gatt == null || bgs == null) {
            System.out.println(gatt);
            System.out.println(bgs);
            return false;
        }

        BluetoothGattCharacteristic characteristic =
                bgs.getCharacteristic(attr);

        if (characteristic == null) {
            Log.w("Characteristic send", "Send characteristic not found");
            return false;
        }

        characteristic.setValue(data);
//        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        return gatt.writeCharacteristic(characteristic);
    }
}
