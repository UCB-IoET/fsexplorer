package pm.storm.fsexplorer;

import android.app.ActionBar;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.UUID;


public class AttributeActivity extends ActionBarActivity {
    private Firestorm tgt;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic tgtChar;
    UUID svc;
    UUID attr;
    String sSvc;
    String sAttr;
    String attrFqn;
    ManifestResolver.ManifestFormatEntry [] fields;
    View [] fieldUI;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attribute);
        Intent intent = getIntent();
        tgt = (Firestorm) intent.getParcelableExtra("firestorm");
        setUIButtonsEnabled(false);
        gatt = tgt.getDevice().connectGatt(this, false, gatt_cb);
        svc = ((ParcelUuid)intent.getParcelableExtra("svc")).getUuid();
        attr = ((ParcelUuid)intent.getParcelableExtra("attr")).getUuid();
        sSvc = svc.toString().substring(4,8);
        sAttr = attr.toString().substring(4,8);
        attrFqn = ManifestResolver.getInstance(this).getAttributeFQN(sSvc, sAttr);
        ((TextView)findViewById(R.id.attrFQN)).setText(attrFqn);
        //Build the display
        genfields();
    }

    public void genfields() {
        fields = ManifestResolver.getInstance(this).getFields(sSvc, sAttr);
        fieldUI = new View[fields.length];
        System.out.println("MFARR "+fields+" len "+fields.length);
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
        LayoutInflater inflater = (LayoutInflater) AttributeActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        fv = inflater.inflate(R.layout.attr_field, null);
        ((TextView) fv.findViewById(R.id.fieldName)).setText(name);
        ((TextView) fv.findViewById(R.id.fieldDesc)).setText(desc);
        ((TextView) fv.findViewById(R.id.type)).setText(type);
        if (isNumeric) {
            ((EditText) fv.findViewById(R.id.editValue)).setInputType(InputType.TYPE_CLASS_NUMBER);
        }
        LinearLayout layout = (LinearLayout) findViewById(R.id.fieldList);
        fv.setId(0);
        fv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(fv);
        return fv;
    }

    @Override
    protected void onPause() {
        super.onPause();
        gatt.disconnect();
    }

    BluetoothGattCallback gatt_cb = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            System.out.println("Connection changed");
            BluetoothGattService s = gatt.getService(svc);
            if (s == null) {
                gatt.discoverServices();
            } else {
                tgtChar = s.getCharacteristic(attr);
                setUIButtonsEnabled(true);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            tgtChar = gatt.getService(svc).getCharacteristic(attr);
            System.out.println("Got char: " + tgtChar);
            setUIButtonsEnabled(true);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            System.out.println("onread");
            final byte [] bval = characteristic.getValue();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i< fields.length; i++) {
                        ManifestResolver.ManifestFormatEntry mfe = fields[i];
                        String val = mfe.getAsString(bval);
                        ((EditText)fieldUI[i].findViewById(R.id.editValue)).setText(val);
                    }
                }
            });
        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            setUIButtonsEnabled(true);
            System.out.println("Got write event: " + status);
            System.out.println("Wanted:"+BluetoothGatt.GATT_SUCCESS);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            //super.onCharacteristicChanged(gatt, characteristic);
            System.out.println("Char changed");
            final byte [] bval = characteristic.getValue();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i< fields.length; i++) {
                        ManifestResolver.ManifestFormatEntry mfe = fields[i];
                        String val = mfe.getAsString(bval);
                        ((EditText)fieldUI[i].findViewById(R.id.editValue)).setText(val);
                    }
                }
            });

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

    private void setUIButtonsEnabled(final boolean v) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.notifyButton).setEnabled(v);
                findViewById(R.id.writeButton).setEnabled(v);
                findViewById(R.id.readButton).setEnabled(v);
            }
        });
    }
    public void wbOnClick(View v) {
        byte [] chardata = new byte[20];
        for (int i = 0; i< fields.length; i++) {
            ManifestResolver.ManifestFormatEntry mfe = fields[i];
            String val = ((EditText)fieldUI[i].findViewById(R.id.editValue)).getText().toString();
            System.out.println("Got sval: " + val);
            mfe.setAsString(chardata, val);
        }
        System.out.println("Assembled chardata: ");
        for (int i = 0; i < 20; i++) {
            System.out.print(String.format(" %02x", chardata[i]));
        }
        System.out.println();
        tgtChar.setValue(chardata);
        gatt.writeCharacteristic(tgtChar);
        setUIButtonsEnabled(false);
    }

    public void rbOnClick(View v) {
        gatt.readCharacteristic(tgtChar);
    }
    protected static final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public void nbOnClick(View v) {
        System.out.println("NB ONCLICK");
        boolean val = ((ToggleButton)v).isChecked();
        System.out.println("SETTING TO"+val);
        BluetoothGattDescriptor desc = tgtChar.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
        desc.setValue(val ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : new byte[] { 0x00, 0x00 });
        gatt.writeDescriptor(desc);
        gatt.setCharacteristicNotification(tgtChar, val);
    }
}
