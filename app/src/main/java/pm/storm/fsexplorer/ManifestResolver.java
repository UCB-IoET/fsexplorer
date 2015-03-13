package pm.storm.fsexplorer;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by immesys on 2/23/15.
 */
public class ManifestResolver
{
    static public class MFDownloader extends AsyncTask<Void, Void, Boolean> {
        Context ctx;
        public MFDownloader(Context ctx) {
            this.ctx = ctx;
        }
        protected void onPreExecute() {

        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                URL canonicalManifest = new URL("https://raw.githubusercontent.com/UCB-IoET/svc/master/manifest.json");
                HttpsURLConnection con = (HttpsURLConnection) canonicalManifest.openConnection();
                InputStream in = new BufferedInputStream(con.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                StringBuilder sb = new StringBuilder();
                String s;

                while ((s = br.readLine()) != null)
                    sb.append(s);
                JSONObject rv = new JSONObject(sb.toString());
                ManifestResolver mr = ManifestResolver.getInstance(ctx);
                mr.updateManifest(rv);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                Toast.makeText(ctx,"Failed to update manifest", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(ctx,"Manifest updated", Toast.LENGTH_LONG).show();
            }
        }

    }
    static public class ManifestService {
        public String name;
        public String author;
        public String desc;
        public String id;
        public String fqn;
        public HashMap<String, ManifestAttribute> attributes;
    }
    static public class ManifestAttribute {
        public String fqn;
        public String id;
        public String name;
        public ManifestFormatEntry [] fields;
        //TODO some stuff regarding the format
    }
    static public class ManifestFormatEntry {
        public enum FType {
            U8, S8, U16, S16, U32, S32, PSTR
        };
        private int offset;
        String strval;
        String numval;
        String name;
        String desc;
        private FType type;
        public int getSize() {
            switch(type) {
                case U8:
                case S8:
                    return 1;
                case U16:
                case S16:
                    return 2;
                case U32:
                case S32:
                    return 4;

            }
            throw new UnsupportedOperationException("PSTR must be at end");
        }
        public void setTypeFromString(String t) {
            switch(t) {
                case "u8":
                    type = FType.U8;
                    break;
                case "s8":
                    type = FType.S8;
                    break;
                case "u16":
                    type = FType.U16;
                    break;
                case "s16":
                    type = FType.S16;
                    break;
                case "u32":
                    type = FType.U32;
                    break;
                case "s32":
                    type = FType.S32;
                    break;
                case "pstr":
                    type = FType.PSTR;
                    break;
                default:
                    throw new IllegalArgumentException("What type is this?: "+t);
            }
        }

        public String getTypeAsString() {
            return type.toString();
        }
        public String getAsString(byte [] data){
            try {
                if (type != FType.PSTR) {
                    long val = 0;

                    switch (type) {
                        case U32:
                        case S32:
                            val += ((long) data[offset + 0] << 0) & 0x000000FF;
                            val += ((long) data[offset + 1] << 8) & 0x0000FF00;
                            val += ((long) data[offset + 2] << 16) & 0x00FF0000;
                            val += ((long) data[offset + 4] << 24) & 0xFF000000;
                            if (type == FType.U32) {
                                return (String.valueOf(val));
                            } else {
                                //I reckon this will make it signed
                                return String.valueOf((int) val);
                            }
                        case U16:
                        case S16:
                            val += ((long) data[offset + 0] << 0) & 0x000000FF;
                            val += ((long) data[offset + 1] << 8) & 0x0000FF00;
                            if (type == FType.U16) {
                                return (String.valueOf(val));
                            } else {
                                //I reckon this will make it signed
                                return String.valueOf((short) val);
                            }
                        case U8:
                        case S8:
                            val += ((long) data[offset + 0] << 0) & 0x000000FF;
                            if (type == FType.U8) {
                                return (String.valueOf(val));
                            } else {
                                //I reckon this will make it signed
                                return String.valueOf((byte) val);
                            }
                    }
                } else {
                    int len = data[offset];
                    System.out.println("Got pstr len"+len);
                    StringBuilder rv = new StringBuilder();
                    for (int i = 0; i < len; i++) {
                        int val = data[offset + i];
                        if (val >= 32 && val < 127) {
                            rv.append((char) val);
                        } else {
                            rv.append('?');
                        }
                    }
                    return rv.toString();
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                return "<ERR>";
            }
            return "";
        }

        public void setAsString(byte [] data, String value) {
            if (type != FType.PSTR) {
                long val = Integer.parseInt(value);

                switch (type) {
                    case U32:
                    case S32:
                        data[offset + 2] = (byte) ((val>>16) & 0xFF);
                        data[offset + 3] = (byte) ((val>>24) & 0xFF);
                    case U16:
                    case S16:
                        data[offset + 1] = (byte) ((val>>8) & 0xFF);
                    case U8:
                    case S8:
                        data[offset + 0] = (byte) (val & 0xFF);
                }
            } else {
                int len = value.length();
                int maxlen = 19 - offset;
                if (maxlen < len) len = maxlen;

                data[offset] = (byte) len;
                for (int i = 0; i < len; i++) {
                    data[offset+1+i] = (byte) value.charAt(i);
                }
            }
        }
        public boolean isNumeric() {
            return type != FType.PSTR;
        }

    }
    private HashMap<String, ManifestService> svcIdToObj;
    private static ManifestResolver instance;
    public static ManifestResolver getInstance(Context ctx)
    {
        if (instance == null) {
            instance = new ManifestResolver(ctx);
        }
        return instance;
    }
    private void updateManifest(JSONObject o) {
        try {
            svcIdToObj = new HashMap<>();

            for (Iterator<String> iter = o.keys(); iter.hasNext(); ) {
                String serviceFQN = iter.next();
                ManifestService msvc = new ManifestService();
                JSONObject svc = o.getJSONObject(serviceFQN);
                msvc.name = svc.getString("name");
                msvc.author = svc.getString("author");
                msvc.desc = svc.getString("desc");
                msvc.id = svc.getString("id").substring(2);
                msvc.fqn = serviceFQN;
                msvc.attributes = new HashMap<>();
                JSONObject attributes = svc.getJSONObject("attributes");
                for (Iterator<String> iter2 = attributes.keys(); iter2.hasNext();) {
                    String attributeFQN = iter2.next();
                    JSONObject attr = attributes.getJSONObject(attributeFQN);
                    ManifestAttribute mattr = new ManifestAttribute();
                    mattr.fqn = attributeFQN;
                    mattr.id = attr.getString("id").substring(2);
                    mattr.name = attr.getString("name");
                    JSONArray fields = attr.getJSONArray("format");
                    System.out.println(">> FIELDS CREATED FOR "+mattr.fqn);
                    mattr.fields = new ManifestFormatEntry[fields.length()];
                    int offset = 0;
                    for (int i = 0; i < fields.length(); i++) {
                        JSONArray field = fields.getJSONArray(i);
                        ManifestFormatEntry mfe = new ManifestFormatEntry();
                        mfe.offset = offset;
                        mfe.setTypeFromString(field.getString(0));
                        mfe.name = field.getString(1);
                        mfe.desc = field.getString(2);
                        mattr.fields[i] = mfe;
                        if(i != fields.length()-1)
                            offset += mfe.getSize();
                    }
                    msvc.attributes.put(mattr.id, mattr);
                }
                svcIdToObj.put(msvc.id, msvc);
            }

        } catch(JSONException e) {
            e.printStackTrace();
        }
    }
    public ManifestFormatEntry[] getFields(String svcId, String attrId) {
        if (svcIdToObj.containsKey(svcId)) {
            ManifestService s = svcIdToObj.get(svcId);
            if (s.attributes.containsKey(attrId)) {
                return s.attributes.get(attrId).fields;
            }
        }
        return new ManifestFormatEntry[]{};
    }
    private ManifestResolver(Context ctx) {
        InputStream is = ctx.getResources().openRawResource(R.raw.manifest);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String s;

            while ((s = br.readLine()) != null)
                sb.append(s);
            JSONObject rv = new JSONObject(sb.toString());
            updateManifest(rv);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //Accessors
    public String getServiceFQN(String svcId) {
        if (svcIdToObj.containsKey(svcId)) {
            return svcIdToObj.get(svcId).fqn;
        }
        return "pm.storm.unk.0x"+svcId;
    }
    public String getServiceName(String svcId) {
        if (svcIdToObj.containsKey(svcId)) {
            return svcIdToObj.get(svcId).name;
        }
        return "Unknown 0x"+svcId;
    }
    public String getServiceDescription(String svcId) {
        if (svcIdToObj.containsKey(svcId)) {
            return svcIdToObj.get(svcId).desc;
        }
        return "Unknown 0x"+svcId;
    }
    public String getAttributeFQN(String svcId, String attrId) {
        if (svcIdToObj.containsKey(svcId)) {
            ManifestService s = svcIdToObj.get(svcId);
            if (s.attributes.containsKey(attrId)) {
                return s.attributes.get(attrId).fqn;
            }
        }
        return "pm.storm.unk.0x"+svcId+".0x"+attrId;
    }
    public String getAttributeName(String svcId, String attrId) {
        if (svcIdToObj.containsKey(svcId)) {
            ManifestService s = svcIdToObj.get(svcId);
            if (s.attributes.containsKey(attrId)) {
                return s.attributes.get(attrId).name;
            }
        }
        return "Unknown 0x"+svcId+"::"+attrId;
    }
}
