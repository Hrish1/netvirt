/*
 * Copyright (c) 2015 - 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netvirt.fibmanager.shell;

import java.util.Locale;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.netvirt.fibmanager.api.IFibManager;
import org.opendaylight.netvirt.fibmanager.api.L3VPNTransportTypes;

@Command(scope = "vpnservice", name = "configureTransportType",
    description = "Configure Preferred Transport Type for L3VPN service")
public class ConfTransportL3VPNCommand extends OsgiCommandSupport {
    private final IFibManager fibManager;

    @Option(name = "-s", aliases = {"--service"}, description = "Service", required = false, multiValued = false)
    String service;

    @Option(name = "-t", aliases = {"--type"}, description = "Configure Transport Type",
        required = false, multiValued = false)
    String transportType;

    public ConfTransportL3VPNCommand(IFibManager fibManager) {
        this.fibManager = fibManager;
    }

    @Override
    protected Object doExecute() {

        if (service == null || service.isEmpty() || !"L3VPN".equalsIgnoreCase(service)) {
            session.getConsole().println("Please provide valid input for service ");
            session.getConsole().println(
                "exec configure-transport-type (-s | --service)  <L3VPN> (-t | --type) <VxLAN/GRE>");
            return null;
        }
        if (transportType == null || transportType.isEmpty()
            || L3VPNTransportTypes.validateTransportType(transportType.toUpperCase(Locale.getDefault()))
                    == L3VPNTransportTypes.Invalid) {
            session.getConsole().println("Please provide valid input for Transport type");
            return null;
        }

        String cachedTransType = fibManager.getConfTransType();
        if (cachedTransType.equalsIgnoreCase(transportType)) {
            session.getConsole().println("Transport type already configured as " + cachedTransType);
            return null;
        }

        if (cachedTransType.equals(L3VPNTransportTypes.Invalid.getTransportType())
            || !fibManager.isVPNConfigured()) {
            fibManager.setConfTransType(service, transportType.toUpperCase(Locale.getDefault()));
            fibManager.writeConfTransTypeConfigDS();
        } else {
            session.getConsole().println("VPN service already configured with " + cachedTransType
                + " as the transport type. Please remove vpn service and configure"
                + " again. Changes were discarded.");
        }
        return null;
    }
}
