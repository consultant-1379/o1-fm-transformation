/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/


package com.ericsson.oss.mediation.fm.o1.engine.instrumentation;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;

import com.ericsson.oss.itpf.sdk.instrument.annotation.InstrumentedBean;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute;

@ApplicationScoped
@InstrumentedBean(displayName = "O1 FM Statistics Monitoring", description = "O1 FM Statistics Monitoring for notifications and supervision")
public class O1EngineStatistics {

    private final AtomicInteger totalNoOfHeartbeatFailures = new AtomicInteger(0);

    private final AtomicLong totalNoOfSuccessfulTransformations = new AtomicLong(0L);

    private final AtomicLong totalNoOfForwardedAlarmEventNotifications = new AtomicLong(0L);

    private final AtomicLong totalNoOfForwardedSyncAlarmEventNotifications = new AtomicLong(0L);

    private final AtomicInteger totalNoOfSupervisedNodes = new AtomicInteger(0);


    public void incrementTotalNoOfHeartbeatFailures() { totalNoOfHeartbeatFailures.incrementAndGet();
    }

    public void decrementTotalNoOfHeartbeatFailures() { totalNoOfHeartbeatFailures.decrementAndGet();
    }

    public void incrementTotalNoOfSuccessfulTransformations() { totalNoOfSuccessfulTransformations.incrementAndGet();
    }

    public void incrementTotalNoOfForwardedAlarmEventNotifications() { totalNoOfForwardedAlarmEventNotifications.incrementAndGet();
    }

    public void incrementTotalNoOfForwardedSyncAlarmEventNotifications(long numberOfEventNotifications) {totalNoOfForwardedSyncAlarmEventNotifications.addAndGet(numberOfEventNotifications);
    }

    public void incrementTotalNoOfSupervisedNodes() { totalNoOfSupervisedNodes.incrementAndGet();
    }

    public void decrementTotalNoOfSupervisedNodes() {  totalNoOfSupervisedNodes.decrementAndGet();
    }

    @MonitoredAttribute(displayName = "Total number of O1Nodes in heartbeat failure",
                        visibility = MonitoredAttribute.Visibility.ALL, units = MonitoredAttribute.Units.NONE,
                        category = MonitoredAttribute.Category.THROUGHPUT, interval = 5,
                        collectionType = MonitoredAttribute.CollectionType.DYNAMIC)
    public int getTotalNoOfHeartbeatFailures() { return totalNoOfHeartbeatFailures.get();
    }

    @MonitoredAttribute(displayName = "Total number of O1Node alarm notifications which were successfully transformed into EventNotifications",
                        visibility = MonitoredAttribute.Visibility.ALL, units = MonitoredAttribute.Units.NONE,
                        category = MonitoredAttribute.Category.THROUGHPUT, interval = 5,
                        collectionType = MonitoredAttribute.CollectionType.DYNAMIC)
    public long getTotalNoOfSuccessfulTransformations() { return totalNoOfSuccessfulTransformations.get();
    }

    @MonitoredAttribute(displayName = "Total number of O1Node alarms successfully forwarded to the FM alarm processor",
                        visibility = MonitoredAttribute.Visibility.ALL, units = MonitoredAttribute.Units.NONE,
                        category = MonitoredAttribute.Category.THROUGHPUT, interval = 5,
                        collectionType = MonitoredAttribute.CollectionType.DYNAMIC)
    public long getTotalNoOfForwardedAlarmEventNotifications() { return totalNoOfForwardedAlarmEventNotifications.get();
    }

    @MonitoredAttribute(displayName = "Total number of O1Node sync alarms successfully forwarded to the FM alarm processor",
                        visibility = MonitoredAttribute.Visibility.ALL, units = MonitoredAttribute.Units.NONE,
                        category = MonitoredAttribute.Category.THROUGHPUT, interval = 5,
                        collectionType = MonitoredAttribute.CollectionType.DYNAMIC)
    public long getTotalNoOfForwardedSyncAlarmEventNotifications() { return totalNoOfForwardedSyncAlarmEventNotifications.get();
    }

    @MonitoredAttribute(displayName = "Total number of O1Nodes supervised for FM",
                        visibility = MonitoredAttribute.Visibility.ALL, units = MonitoredAttribute.Units.NONE,
                        category = MonitoredAttribute.Category.THROUGHPUT, interval = 5,
                        collectionType = MonitoredAttribute.CollectionType.DYNAMIC)
    public int getTotalNoOfSupervisedNodes() { return totalNoOfSupervisedNodes.get();
    }

}
