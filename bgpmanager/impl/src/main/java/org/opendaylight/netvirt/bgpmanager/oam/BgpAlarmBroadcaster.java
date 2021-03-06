/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netvirt.bgpmanager.oam;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import javax.management.AttributeChangeNotification;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class BgpAlarmBroadcaster extends NotificationBroadcasterSupport
        implements BgpAlarmBroadcasterMBean, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BgpAlarmBroadcaster.class);
    private final AtomicLong sequenceNumber = new AtomicLong(1);

    private static ObjectName objectName() throws MalformedObjectNameException {
        return new ObjectName("SDNC.FM:name=BgpAlarmObj");
    }

    @PostConstruct
    public void init() {
        // Set up the Infra for Posting BGP Alarms as JMX notifications.
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            mbs.registerMBean(this, objectName());
        } catch (JMException e) {
            LOG.error("Adding a NotificationBroadcaster failed.", e);
        }
        LOG.info("{} start", getClass().getSimpleName());
    }

    @Override
    @PreDestroy
    public void close() {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName objectName = objectName();
            if (mbs.isRegistered(objectName)) {
                mbs.unregisterMBean(objectName);
            }
        } catch (JMException e) {
            LOG.error("Error unregistering MXBean", e);
        }
    }

    @Override
    public void sendBgpAlarmInfo(String pfx, int subcode) {
        BgpAlarmErrorCodes userAlarm = BgpAlarmErrorCodes.checkErrorSubcode(subcode);
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(userAlarm.getAlarmType());
        arrayList.add("Peer=" + pfx);
        arrayList.add("BGF");
        sendNotification(new AttributeChangeNotification(this, sequenceNumber.incrementAndGet(),
            System.currentTimeMillis(), "raise Alarm Object notified", "raiseAlarmObject", "ArrayList", "", arrayList));
        LOG.info("BGP: Alarm {} has been posted.", userAlarm.getAlarmType());
    }
}
