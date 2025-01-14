/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * api.godaddy.com
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: 2.4.9
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.infra.godaddy.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.optio3.infra.godaddy.model.CheckType;
import com.optio3.infra.godaddy.model.CountryCode;
import com.optio3.infra.godaddy.model.DNSRecord;
import com.optio3.infra.godaddy.model.DomainAvailableBulk;
import com.optio3.infra.godaddy.model.DomainAvailableResponse;
import com.optio3.infra.godaddy.model.DomainContacts;
import com.optio3.infra.godaddy.model.DomainDetail;
import com.optio3.infra.godaddy.model.DomainPurchase;
import com.optio3.infra.godaddy.model.DomainPurchaseResponse;
import com.optio3.infra.godaddy.model.DomainRenew;
import com.optio3.infra.godaddy.model.DomainStatus;
import com.optio3.infra.godaddy.model.DomainStatusGroup;
import com.optio3.infra.godaddy.model.DomainSuggestion;
import com.optio3.infra.godaddy.model.DomainSummary;
import com.optio3.infra.godaddy.model.DomainTransferIn;
import com.optio3.infra.godaddy.model.DomainUpdate;
import com.optio3.infra.godaddy.model.DomainsContactsBulk;
import com.optio3.infra.godaddy.model.IdentityDocumentCreate;
import com.optio3.infra.godaddy.model.IdentityDocumentId;
import com.optio3.infra.godaddy.model.IdentityDocumentSummary;
import com.optio3.infra.godaddy.model.IdentityDocumentVerification;
import com.optio3.infra.godaddy.model.IncludeKind;
import com.optio3.infra.godaddy.model.JsonSchema;
import com.optio3.infra.godaddy.model.LegalAgreement;
import com.optio3.infra.godaddy.model.PrivacyPurchase;
import com.optio3.infra.godaddy.model.RecordType;
import com.optio3.infra.godaddy.model.SourcesKind;
import com.optio3.infra.godaddy.model.TldSummary;
import org.apache.cxf.jaxrs.ext.PATCH;

@Path("/")
public interface DefaultApi
{
    @GET
    @Path("/v1/domains/available")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public DomainAvailableResponse available(@QueryParam("domain") String domain,
                                             @QueryParam("checkType") CheckType checkType,
                                             @QueryParam("forTransfer") Boolean forTransfer);

    @POST
    @Path("/v1/domains/available")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public DomainAvailableBulk availableBulk(List<String> domains,
                                             @QueryParam("checkType") CheckType checkType);

    @DELETE
    @Path("/v1/domains/{domain}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public void cancel(@PathParam("domain") String domain);

    @DELETE
    @Path("/v1/domains/{domain}/privacy")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public void cancelPrivacy(@PathParam("domain") String domain);

    @POST
    @Path("/v1/domains/contacts/validate")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public void contactsValidate(DomainsContactsBulk body,
                                 @HeaderParam("X-Private-Label-Id") Integer xPrivateLabelId,
                                 @QueryParam("marketId") String marketId);

    @POST
    @Path("/v1/domains/identityDocuments")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public IdentityDocumentId createIdentityDocument(IdentityDocumentCreate identityDocument);

    @POST
    @Path("/v1/domains/identityDocuments/{identityDocumentId}/verifications")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public List<IdentityDocumentVerification> createVerification(@PathParam("identityDocumentId") String identityDocumentId,
                                                                 @QueryParam("tlds") List<String> tlds);

    @GET
    @Path("/v1/domains/{domain}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public DomainDetail get(@PathParam("domain") String domain);

    @GET
    @Path("/v1/domains/agreements")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public List<LegalAgreement> getAgreement(@QueryParam("tlds") List<String> tlds,
                                             @QueryParam("privacy") Boolean privacy,
                                             @HeaderParam("X-Market-Id") String xMarketId,
                                             @QueryParam("forTransfer") Boolean forTransfer);

    @GET
    @Path("/v1/domains/identityDocuments/{identityDocumentId}/verifications")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public List<IdentityDocumentVerification> getIdentityDocumentVerification(@PathParam("identityDocumentId") String identityDocumentId,
                                                                              @QueryParam("tlds") List<String> tlds);

    @GET
    @Path("/v1/domains")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public List<DomainSummary> list(@QueryParam("statuses") List<DomainStatus> statuses,
                                    @QueryParam("statusGroups") List<DomainStatusGroup> statusGroups,
                                    @QueryParam("limit") Integer limit,
                                    @QueryParam("marker") String marker,
                                    @QueryParam("includes") List<IncludeKind> includes,
                                    @QueryParam("modifiedDate") String modifiedDate);

    @GET
    @Path("/v1/domains/identityDocuments")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public List<IdentityDocumentSummary> listIdentityDocuments(@HeaderParam("X-Shopper-Id") String xShopperId);

    @POST
    @Path("/v1/domains/purchase")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public DomainPurchaseResponse purchase(DomainPurchase body);

    @POST
    @Path("/v1/domains/{domain}/privacy/purchase")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public DomainPurchaseResponse purchasePrivacy(@PathParam("domain") String domain,
                                                  PrivacyPurchase body);

    @PATCH
    @Path("/v1/domains/{domain}/records")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public void recordAdd(@PathParam("domain") String domain,
                          List<DNSRecord> records);

    @GET
    @Path("/v1/domains/{domain}/records")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public List<DNSRecord> recordGet(@PathParam("domain") String domain,
                                     @QueryParam("offset") Integer offset,
                                     @QueryParam("limit") Integer limit);

    @GET
    @Path("/v1/domains/{domain}/records/{type}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public List<DNSRecord> recordGetWithType(@PathParam("domain") String domain,
                                             @PathParam("type") RecordType type,
                                             @QueryParam("offset") Integer offset,
                                             @QueryParam("limit") Integer limit);

    @GET
    @Path("/v1/domains/{domain}/records/{type}/{name}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public List<DNSRecord> recordGetWithTypeName(@PathParam("domain") String domain,
                                                 @PathParam("type") RecordType type,
                                                 @PathParam("name") String name,
                                                 @QueryParam("offset") Integer offset,
                                                 @QueryParam("limit") Integer limit);

    @PUT
    @Path("/v1/domains/{domain}/records")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public void recordReplace(@PathParam("domain") String domain,
                              List<DNSRecord> records);

    @PUT
    @Path("/v1/domains/{domain}/records/{type}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public void recordReplaceType(@PathParam("domain") String domain,
                                  @PathParam("type") RecordType type,
                                  List<DNSRecord> records);

    @PUT
    @Path("/v1/domains/{domain}/records/{type}/{name}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public void recordReplaceTypeName(@PathParam("domain") String domain,
                                      @PathParam("type") RecordType type,
                                      @PathParam("name") String name,
                                      List<DNSRecord> records);

    @DELETE
    @Path("/v1/domains/{domain}/records/{type}/{name}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public void recordDeleteTypeName(@PathParam("domain") String domain,
                                     @PathParam("type") RecordType type,
                                     @PathParam("name") String name);

    @POST
    @Path("/v1/domains/{domain}/renew")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public DomainPurchaseResponse renew(@PathParam("domain") String domain,
                                        DomainRenew body);

    @GET
    @Path("/v1/domains/purchase/schema/{tld}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public JsonSchema schema(@PathParam("tld") String tld);

    @GET
    @Path("/v1/domains/suggest")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public List<DomainSuggestion> suggest(@QueryParam("query") String query,
                                          @QueryParam("country") CountryCode country,
                                          @QueryParam("city") String city,
                                          @QueryParam("sources") List<SourcesKind> sources,
                                          @QueryParam("tlds") List<String> tlds,
                                          @QueryParam("lengthMax") Integer lengthMax,
                                          @QueryParam("lengthMin") Integer lengthMin,
                                          @QueryParam("limit") Integer limit,
                                          @QueryParam("waitMs") Integer waitMs);

    @GET
    @Path("/v1/domains/tlds")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public List<TldSummary> tlds();

    @POST
    @Path("/v1/domains/{domain}/transfer")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public DomainPurchaseResponse transferIn(@PathParam("domain") String domain,
                                             DomainTransferIn body);

    @PATCH
    @Path("/v1/domains/{domain}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public void update(@PathParam("domain") String domain,
                       DomainUpdate body);

    @PATCH
    @Path("/v1/domains/{domain}/contacts")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public void updateContacts(@PathParam("domain") String domain,
                               DomainContacts contacts);

    @POST
    @Path("/v1/domains/purchase/validate")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public void validate(DomainPurchase body);

    @POST
    @Path("/v1/domains/{domain}/verifyRegistrantEmail")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public void verifyEmail(@PathParam("domain") String domain);
}
