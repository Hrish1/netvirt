/*
 * Copyright (c) 2015 - 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netvirt.fibmanager.api;

import com.google.common.util.concurrent.FutureCallback;

import java.math.BigInteger;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.fibmanager.rev150330.RouterInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.fibmanager.rev150330.vrfentries.VrfEntry;

public interface IFibManager {
    void populateFibOnNewDpn(BigInteger dpnId, long vpnId, String rd,
                             FutureCallback<List<Void>> callback);

    void cleanUpExternalRoutesOnDpn(BigInteger dpnId, long vpnId, String rd,
                                    String localNextHopIp, String remoteNextHopIp);

    void populateExternalRoutesOnDpn(BigInteger localDpnId, long vpnId, String rd,
                                     String localNextHopIp, String remoteNextHopIp);

    void cleanUpInternalRoutesOnDpn(BigInteger dpnId, long vpnId, String rd,
                                    String localNextHopIp, String remoteNextHopIp);

    void populateInternalRoutesOnDpn(BigInteger localDpnId, long vpnId, String rd,
                                     String localNextHopIp, String remoteNextHopIp);

    void cleanUpDpnForVpn(BigInteger dpnId, long vpnId, String rd,
                          FutureCallback<List<Void>> callback);

    // TODO Feels like this method is not used anywhere
    void addStaticRoute(String vpnName, String prefix, String nextHop, String rd, int label);

    void deleteStaticRoute(String vpnName, String prefix, String nextHop, String rd);

    void setConfTransType(String service, String transportType);

    String getConfTransType();

    boolean isVPNConfigured();

    void writeConfTransTypeConfigDS();

    String getReqTransType();

    String getTransportTypeStr(String tunType);

    void manageRemoteRouteOnDPN(boolean action,
                                BigInteger localDpnId,
                                long vpnId,
                                String rd,
                                String destPrefix,
                                String destTepIp,
                                long label);

    void addOrUpdateFibEntry(DataBroker broker, String rd, String macAddress, String prefix, List<String> nextHopList,
                             VrfEntry.EncapType encapType, long label, long l3vni, String gwMacAddress,
                             String parentVpnRd, RouteOrigin origin, WriteTransaction writeConfigTxn);

    void addFibEntryForRouterInterface(DataBroker broker, String rd, String prefix,
                                       RouterInterface routerInterface, long label, WriteTransaction writeConfigTxn);

    void removeOrUpdateFibEntry(DataBroker broker, String rd, String prefix, String nextHopToRemove,
                                WriteTransaction writeConfigTxn);

    void removeFibEntry(DataBroker broker, String rd, String prefix, WriteTransaction writeConfigTxn);

    void updateRoutePathForFibEntry(DataBroker broker, String rd, String prefix, String nextHop,
                        long label, boolean nextHopAdd, WriteTransaction writeConfigTxn);

    void addVrfTable(DataBroker broker, String rd, WriteTransaction writeConfigTxn);

    void removeVrfTable(DataBroker broker, String rd, WriteTransaction writeConfigTxn);

    void removeInterVPNLinkRouteFlows(String interVpnLinkName,
                                      boolean isVpnFirstEndPoint,
                                      VrfEntry vrfEntry);

    void programDcGwLoadBalancingGroup(List<String> availableDcGws, BigInteger dpnId,
            String destinationIp, int addRemoveOrUpdate, boolean isTunnelUp);

    void refreshVrfEntry(String rd, String prefix);
}
