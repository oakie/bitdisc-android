package nu.ekskog.bitdisc;

import java.util.HashMap;
import java.util.Map;

public class Entity {
    private Map<String, Object> mProperties = new HashMap<String, Object>();
    private String mType;

    public Entity(String type) {
        mType = type;
    }

    public Entity(Entity other) {
        mType = other.getType();
        for(String key : other.getProperties().keySet())
            mProperties.put(key, other.get(key));
    }

    public void setType(String type) {
        mType = type;
    }

    public String getType() {
        return mType;
    }

    public void put(String key, Object value) {
        mProperties.put(key, value);
    }

    public Object get(String key) {
        return mProperties.get(key);
    }

    public Object remove(String key) {
        return mProperties.remove(key);
    }

    public boolean has(String key) {
        return mProperties.get(key) != null;
    }

    public Map<String, Object> getProperties() {
        return mProperties;
    }

    public String toString() {
        String s = "entity-" + mType;
        for(String key : mProperties.keySet())
            s += "," + key + ":" + mProperties.get(key);
        return s;
    }
}
