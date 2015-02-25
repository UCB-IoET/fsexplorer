package pm.storm.fsexplorer;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by immesys on 2/23/15.
 */
public class Firestorm implements Parcelable
{
    private BluetoothDevice bd;
    private byte [] advdata;
    private int rssi;
    private String addr;
    private HashMap<UUID, ArrayList<UUID>> services;

    public Firestorm(BluetoothDevice bd, int rssi, byte [] advdata) {
        this.bd = bd;
        this.rssi = rssi;
        this.addr = bd.getAddress();
        this.advdata = advdata;
        services = new HashMap<>();
    }

    @Override
    public String toString(){
        return (String.format("Firestorm [%3d] : %s\n"+getShortServices(),rssi, getAddr()));
    }

    public String getShortServices() {
        StringBuilder rv = new StringBuilder();
        int count = 0;
        for (UUID u : getServices()) {
            String s = u.toString();
            if (s.substring(4,8).equals("1800") || s.substring(4,8).equals("1801")) {
                continue; //Ignore the BLE services
            }
            if (count < 4) {
                rv.append("0x"+s.substring(4,8)+" ");
            }
            count++;
        }
        if (count >= 4) {
            rv.append("+"+(count-3));
        }
        return rv.toString();
    }
    public String getAdvHexString() {
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<advdata.length;i++) {
            int v = advdata[i];
            sb.append(String.format("%02x ", v&0xFF));
        }
        return sb.toString().trim();
    }

    //Returns true if this resulted in the addition of a new service
    public boolean addServiceUuid(UUID u) {
        if (u.toString().substring(4,8).equals("1800") || u.toString().substring(4,8).equals("1801") ) return false;
        if (!services.containsKey(u)) {
            services.put(u, new ArrayList<UUID>());
            return true;
        }
        return false;
    }

    public boolean addAttributeUuid(UUID svc, UUID attribute) {
        if (svc.toString().substring(4,8).equals("1800") || svc.toString().substring(4,8).equals("1801") ) return false;
        boolean rv = addServiceUuid(svc);
        if (!services.get(svc).contains(attribute)){
            services.get(svc).add(attribute);
            return true;
        }
        return rv;
    }

    public ArrayList<UUID> getAttributes(UUID svc) {
        return services.get(svc);
    }
    public List<UUID> getServices() {
        ArrayList<UUID> rv = new ArrayList<>(services.keySet());
        Collections.sort(rv);
        return rv;
    }

    public String getAdvString() {
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<advdata.length;i++)
        {
            int v = advdata[i];
            if (v < 32 || v > 126) {
                sb.append("\u058D");
            }
            else
            {
                sb.append((char)v);
            }
        }
        return sb.toString();
    }
    @Override
    public boolean equals(Object rhs){
        if (rhs instanceof Firestorm) {
            return this.getAddr().equals(((Firestorm)rhs).getAddr());
        }
        return false;
    }

    public String getAddr() {
        return addr;
    }

    public int describeContents() {
        return 0;
    }

    public BluetoothDevice getDevice() {
        return bd;
    }

    public static final Parcelable.Creator<Firestorm> CREATOR
            = new Parcelable.Creator<Firestorm>() {
        public Firestorm createFromParcel(Parcel in) {
            return new Firestorm(in);
        }

        public Firestorm[] newArray(int size) {
            return new Firestorm[size];
        }
    };

    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(this.bd, 0);
        out.writeByteArray(this.advdata);
        out.writeInt(this.rssi);
        out.writeString(this.addr);
        out.writeSerializable(this.services);
    }

    private Firestorm(Parcel in) {
        this.bd = in.readParcelable(null);
        this.advdata = in.createByteArray();
        this.rssi = in.readInt();
        this.addr = in.readString();
        //TODO replace with 'correct' solution
        this.services = (HashMap<UUID, ArrayList<UUID>>) in.readSerializable();
    }

}
