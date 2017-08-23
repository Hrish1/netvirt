/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netvirt.coe;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.pod.rev170611.Coe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.pod.rev170611.coe.Pods;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class PodListener implements DataTreeChangeListener<Pods> {

    private static final Logger LOG = LoggerFactory.getLogger(PodListener.class);
    private ListenerRegistration<PodListener> listenerRegistration;
    private final DataBroker dataBroker;

    @Inject
    public PodListener(final DataBroker dataBroker) {
        registerListener(LogicalDatastoreType.CONFIGURATION, dataBroker);
        this.dataBroker = dataBroker;
    }

    protected InstanceIdentifier<Pods> getWildCardPath() {
        return InstanceIdentifier.create(Coe.class).child(Pods.class);
    }

    public void registerListener(LogicalDatastoreType dsType, final DataBroker db) {
        final DataTreeIdentifier<Pods> treeId = new DataTreeIdentifier<>(dsType, getWildCardPath());
        listenerRegistration = db.registerDataTreeChangeListener(treeId, PodListener.this);
    }

    @PreDestroy
    public void close() {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } finally {
                listenerRegistration = null;
            }
        }
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<Pods>> changes) {
        for (DataTreeModification<Pods> change : changes) {
            final InstanceIdentifier<Pods> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<Pods> mod = change.getRootNode();

            switch (mod.getModificationType()) {
                case DELETE:
                    LOG.info("Pod deleted {}", mod.getDataBefore());
                    break;
                case SUBTREE_MODIFIED:
                    LOG.info("Pod updated old : {}, new : {}", mod.getDataBefore(), mod.getDataAfter());
                    break;
                case WRITE:
                    if (mod.getDataBefore() == null) {
                        LOG.info("Pod added {}", mod.getDataAfter());
                    } else {
                        LOG.info("Pod updated old : {}, new : {}", mod.getDataBefore(), mod.getDataAfter());
                    }
                    break;
                default:
                    // FIXME: May be not a good idea to throw.
                    throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
            }
        }
    }
}