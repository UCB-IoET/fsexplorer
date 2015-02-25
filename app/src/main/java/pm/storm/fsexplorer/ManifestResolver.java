package pm.storm.fsexplorer;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

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
        //TODO some stuff regarding the format
    }
    static public class ManifestFormatEntry {
        public enum FType {
            U8, S8, U16, S16, U32, S32, PSTR
        };
        private int offset;
        private FType type;
        public String getAsString(byte [] data){
            if (type != FType.PSTR) {
                int val;
                /*
                switch(type) {
                    case U16
                }*/
            }
            return "";
        }
        public void setAsString(byte [] data, String value) {

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
                    msvc.attributes.put(mattr.id, mattr);
                }
                svcIdToObj.put(msvc.id, msvc);
            }

        } catch(JSONException e) {
            e.printStackTrace();
        }
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
