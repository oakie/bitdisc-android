package nu.ekskog.bitdisc.services;

import nu.ekskog.bitdisc.models.Entity;

public interface IBitdiscListener {
    public void newCloudData(String type, Entity entity);
}
