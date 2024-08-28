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

package com.ericsson.oss.mediation.fm.o1.instrumentation;

import com.ericsson.oss.itpf.sdk.instrument.annotation.InstrumentedBean;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


@ApplicationScoped
@InstrumentedBean(displayName = "O1 FM Handler Monitoring", description = "O1 FM Handler Monitoring")
public class O1HandlerStatistics {

    private final AtomicLong totalNoOfAlarmsReceived = new AtomicLong(0L);

    private final AtomicLong totalNoOfHeartbeatsReceived = new AtomicLong(0L);

    public void incrementTotalNoOfAlarmsReceived() { totalNoOfAlarmsReceived.incrementAndGet();
    }

    public void incrementTotalNoOfHeartbeatsReceived() { totalNoOfHeartbeatsReceived.incrementAndGet();
    }

    @MonitoredAttribute(displayName = "Total number of spontaneous alarms received from O1Nodes",
                        visibility = MonitoredAttribute.Visibility.ALL, units = MonitoredAttribute.Units.NONE,
                        category = MonitoredAttribute.Category.THROUGHPUT, interval = 5,
                        collectionType = MonitoredAttribute.CollectionType.DYNAMIC)
    public long getTotalNoOfAlarmsReceived() { return totalNoOfAlarmsReceived.get();
    }

    @MonitoredAttribute(displayName = "Total number of heartbeat notifications received from O1Nodes",
                        visibility = MonitoredAttribute.Visibility.ALL, units = MonitoredAttribute.Units.NONE,
                        category = MonitoredAttribute.Category.THROUGHPUT, interval = 5,
                        collectionType = MonitoredAttribute.CollectionType.DYNAMIC)
    public long getTotalNoOfHeartbeatsReceived() { return totalNoOfHeartbeatsReceived.get();
    }

}
