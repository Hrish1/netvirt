/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netvirt.openstack.sfc.translator.flowclassifier;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.netvirt.openstack.sfc.translator.INeutronSfcDataProcessor;
import org.opendaylight.netvirt.openstack.sfc.translator.NeutronMdsalHelper;
import org.opendaylight.netvirt.openstack.sfc.translator.SfcMdsalHelper;
import org.opendaylight.netvirt.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.Ipv4Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.Ipv6Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.AclKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.AccessListEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.AceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.AceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.AceIpVersion;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160218.acl.transport.header.fields.DestinationPortRange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160218.acl.transport.header.fields.DestinationPortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160218.acl.transport.header.fields.SourcePortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.EthertypeV4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.EthertypeV6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolTcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolUdp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.sfc.flow.classifier.rev160511.sfc.flow.classifiers.attributes.sfc.flow.classifiers.SfcFlowClassifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;

/**
 * Class will convert OpenStack Flow Classifier API yang models to
 * OpenDaylight ACL yang models.
 */
public class FlowClassifierTranslator implements INeutronSfcDataProcessor<SfcFlowClassifier>{
    private static final Logger LOG = LoggerFactory.getLogger(FlowClassifierTranslator.class);
    private static final Short PROTO_TCP = 6;
    private static final Short PROTO_UDP = 17;
    private static final String RULE = "_rule";

    private final DataBroker db;
    private NeutronFlowClassifierListener neutronFlowClassifierListener;
    private final SfcMdsalHelper sfcMdsalHelper;
    private final NeutronMdsalHelper neutronMdsalHelper;

    public FlowClassifierTranslator(DataBroker db) {
        this.db = db;
        sfcMdsalHelper = new SfcMdsalHelper(db);
        neutronMdsalHelper = new NeutronMdsalHelper(db);
    }

    public void start() {
        LOG.info("Flow Classifier Translator Initialized.");
        if(neutronFlowClassifierListener == null) {
            neutronFlowClassifierListener = new NeutronFlowClassifierListener(db, this);
        }
    }

    /**
     * Method removes Acl respective to SfcFlowClassifier which is identified by InstanceIdentifier.
     *
     * @param path - the whole path to SfcFlowClassifier
     * @param deletedSfcFlowClassifier        - SfcFlowClassifier for removing
     */
    @Override
    public void remove(InstanceIdentifier<SfcFlowClassifier> path, SfcFlowClassifier deletedSfcFlowClassifier) {
        Acl aclFlowClassifier = buildAcl(deletedSfcFlowClassifier);
        sfcMdsalHelper.removeAclFlowClassifier(aclFlowClassifier);
    }

    /**
     * Method updates the original SfcFlowClassifier to the update SfcFlowClassifier.
     * Both are identified by same InstanceIdentifier.
     *
     * @param path - the whole path to SfcFlowClassifier
     * @param originalSfcFlowClassifier   - original SfcFlowClassifier (for update)
     * @param updatedSfcFlowClassifier     - changed SfcFlowClassifier (contain updates)
     */
    @Override
    public void update(InstanceIdentifier<SfcFlowClassifier> path,
                       SfcFlowClassifier originalSfcFlowClassifier,
                       SfcFlowClassifier updatedSfcFlowClassifier) {

        Acl aclFlowClassifier = buildAcl(updatedSfcFlowClassifier);
        sfcMdsalHelper.updateAclFlowClassifier(aclFlowClassifier);
    }

    /**
     * Method adds the SfcFlowClassifier which is identified by InstanceIdentifier
     * to device.
     *
     * @param path - the whole path to new SfcFlowClassifier
     * @param sfcFlowClassifier        - new SfcFlowClassifier
     */
    @Override
    public void add(InstanceIdentifier<SfcFlowClassifier> path, SfcFlowClassifier sfcFlowClassifier) {
        Acl aclFlowClassifier = buildAcl(sfcFlowClassifier);
        sfcMdsalHelper.addAclFlowClassifier(aclFlowClassifier);
    }

    private Acl buildAcl(SfcFlowClassifier flowClassifier) {
        LOG.info("OpenStack Networkgin SFC pushed Flow classfier : {}", flowClassifier);
        AclBuilder aclBuilder = new AclBuilder();
        AccessListEntriesBuilder accessListEntriesBuilder = new AccessListEntriesBuilder();
        AceBuilder aceBuilder = new AceBuilder();
        MatchesBuilder matchesBuilder = new MatchesBuilder();
        AceIpBuilder aceIpBuilder = new AceIpBuilder();
        DestinationPortRangeBuilder destinationPortRange = new DestinationPortRangeBuilder();
        SourcePortRangeBuilder sourcePortRangeBuilder = new SourcePortRangeBuilder();

        if (flowClassifier.getUuid() != null) {
            if (flowClassifier.getName() != null) {
                aclBuilder.setAclName(flowClassifier.getUuid().getValue() + "_" + flowClassifier.getName());
            } else {
                aclBuilder.setAclName(flowClassifier.getUuid().getValue());
            }

        }
        if (flowClassifier.getEthertype() != null) {
            IpPrefix sourceIp = null;
            IpPrefix destinationIp = null;
            if (flowClassifier.getSourceIpPrefix() != null) {
                sourceIp = flowClassifier.getSourceIpPrefix();
            }
            if (flowClassifier.getDestinationIpPrefix() != null) {
                destinationIp = flowClassifier.getDestinationIpPrefix();
            }
            if (flowClassifier.getEthertype() == EthertypeV4.class) {
                AceIpv4Builder aceIpv4Builder = new AceIpv4Builder();
                if (sourceIp.getIpv4Prefix() != null ) {
                    aceIpv4Builder.setSourceIpv4Network(sourceIp.getIpv4Prefix());
                }
                if (destinationIp.getIpv4Prefix() != null) {
                    aceIpv4Builder.setDestinationIpv4Network(destinationIp.getIpv4Prefix());
                }
                aceIpBuilder.setAceIpVersion(aceIpv4Builder.build());
                aclBuilder.setAclType(Ipv4Acl.class);
            }
            if (flowClassifier.getEthertype() == EthertypeV6.class) {
                AceIpv6Builder aceIpv6Builder = new AceIpv6Builder();
                if (sourceIp.getIpv6Prefix() != null ) {
                    aceIpv6Builder.setSourceIpv6Network(sourceIp.getIpv6Prefix());
                }
                if (destinationIp.getIpv6Prefix() != null) {
                    aceIpv6Builder.setDestinationIpv6Network(destinationIp.getIpv6Prefix());
                }
                aceIpBuilder.setAceIpVersion(aceIpv6Builder.build());
                aclBuilder.setAclType(Ipv6Acl.class);
            }
        }
        if (flowClassifier.getProtocol() != null) {
            if (flowClassifier.getProtocol() == ProtocolTcp.class) {
                aceIpBuilder.setProtocol(PROTO_TCP);
            }
            if (flowClassifier.getProtocol() == ProtocolUdp.class) {
                aceIpBuilder.setProtocol(PROTO_UDP);
            }
        }
        if (flowClassifier.getSourcePortRangeMin() != null) {
            sourcePortRangeBuilder.setLowerPort(new PortNumber(flowClassifier.getSourcePortRangeMin()));
        }
        if (flowClassifier.getSourcePortRangeMax() != null) {
            sourcePortRangeBuilder.setUpperPort(new PortNumber(flowClassifier.getSourcePortRangeMax()));
        }
        if (flowClassifier.getDestinationPortRangeMin() != null) {
            destinationPortRange.setLowerPort(new PortNumber(flowClassifier.getDestinationPortRangeMin()));
        }
        if (flowClassifier.getDestinationPortRangeMax() != null) {
            destinationPortRange.setUpperPort(new PortNumber(flowClassifier.getDestinationPortRangeMax()));
        }
        if (flowClassifier.getLogicalSourcePort() != null) {
            // No respective ACL construct for it.
        }
        if (flowClassifier.getLogicalDestinationPort() != null) {
            // No respective ACL construct for it.
        }
        if (flowClassifier.getL7Parameter() != null) {
            //It's currently not supported.
        }

        aceIpBuilder.setSourcePortRange(sourcePortRangeBuilder.build());
        aceIpBuilder.setDestinationPortRange(destinationPortRange.build());

        matchesBuilder.setAceType(aceIpBuilder.build());

        aceBuilder.setMatches(matchesBuilder.build());

        //OpenStack networking-sfc don't pass action information
        //with flow classifier. It need to be determined using the
        //Port Chain data and then flow calssifier need to be updated
        //with the actions.

        aceBuilder.setRuleName(aclBuilder.getAclName() + RULE);
        aceBuilder.setKey(new AceKey(aceBuilder.getRuleName()));

        ArrayList<Ace> aceList = new ArrayList<>();
        aceList.add(aceBuilder.build());
        accessListEntriesBuilder.setAce(aceList);

        aclBuilder.setAccessListEntries(accessListEntriesBuilder.build());
        aclBuilder.setKey(new AclKey(aclBuilder.getAclName(),aclBuilder.getAclType()));

        LOG.info("Translated ACL Flow classfier : {}", aclBuilder);

        return aclBuilder.build();
    }
}