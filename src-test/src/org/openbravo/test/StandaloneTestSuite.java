/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2009-2024 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.openbravo.advpaymentmngt.test.DocumentNumberGeneration;
import org.openbravo.authentication.hashing.PasswordHashing;
import org.openbravo.authentication.oauth2.OAuth2SignInProviderTest;
import org.openbravo.authentication.oauth2.OAuth2TokenAuthenticationManagerTest;
import org.openbravo.base.model.NamingUtilTest;
import org.openbravo.base.weld.test.testinfrastructure.CdiInfrastructure;
import org.openbravo.base.weld.test.testinfrastructure.DalPersistanceEventTest;
import org.openbravo.base.weld.test.testinfrastructure.ParameterizedCdi;
import org.openbravo.base.weld.test.testinfrastructure.ParameterizedCdiWithSkip;
import org.openbravo.cache.TimeInvalidatedCacheTest;
import org.openbravo.client.application.attachment.ReprintableDocumentTest;
import org.openbravo.client.application.test.ADCSInitialization;
import org.openbravo.client.application.test.ADCSTest;
import org.openbravo.client.application.test.ApplicationTest;
import org.openbravo.client.application.test.DisplayLogicAtServerLevelTest;
import org.openbravo.client.application.test.DynamicExpressionParserTest;
import org.openbravo.client.application.test.GenerateTypesJSTest;
import org.openbravo.client.application.test.MenuTemplateTest;
import org.openbravo.client.application.test.MenuTest;
import org.openbravo.client.application.test.SelectionPoolConfigurationTest;
import org.openbravo.client.application.test.event.DatasourceEventObserver;
import org.openbravo.client.application.test.event.PersistanceObserver;
import org.openbravo.client.application.window.OBViewFieldHandlerFieldSorterTest;
import org.openbravo.client.kernel.CSSMinifier;
import org.openbravo.client.kernel.freemarker.test.FreemarkerTemplateProcessorTest;
import org.openbravo.client.kernel.freemarker.test.GenerateComponentTest;
import org.openbravo.client.kernel.freemarker.test.LabelTest;
import org.openbravo.client.kernel.reference.materialmgmt.RelevantCharacteristicAdditionalPropertyResolverTest;
import org.openbravo.client.kernel.reference.materialmgmt.RelevantCharacteristicQueryHookTest;
import org.openbravo.client.kernel.test.CompressionTest;
import org.openbravo.dal.security.SecurityCheckerTest;
import org.openbravo.dal.service.DataPoolCheckerTest;
import org.openbravo.erpCommon.info.ClassicSelectorTest;
import org.openbravo.erpCommon.utility.OBDateUtilsTest;
import org.openbravo.event.CharacteristicEventHandlerTest;
import org.openbravo.event.ProductEventHandlerTest;
import org.openbravo.materialmgmt.PurchaseDocumentLineTest;
import org.openbravo.materialmgmt.RelevantCharacteristicFieldTerminologyChecker;
import org.openbravo.materialmgmt.RelevantCharacteristicPropertyTest;
import org.openbravo.scheduling.ProcessBundleSerializationTest;
import org.openbravo.scheduling.ProcessSchedulingTest;
import org.openbravo.scheduling.trigger.MisfirePolicyTest;
import org.openbravo.scheduling.trigger.TriggerProviderTest;
import org.openbravo.service.datasource.ModelDataSourceServiceTest;
import org.openbravo.service.externalsystem.ExternalSystemProviderTest;
import org.openbravo.service.externalsystem.http.HttpExternalSystemDataCreationTest;
import org.openbravo.service.externalsystem.http.OAuth2AccessTokenTest;
import org.openbravo.service.json.AdvancedQueryBuilderTest;
import org.openbravo.service.json.JSONWriterToCSVTest;
import org.openbravo.service.json.JsonUtilsTest;
import org.openbravo.service.json.OrganizationTimeZoneTest;
import org.openbravo.synchronization.event.EventTriggeringTest;
import org.openbravo.synchronization.event.SynchronizationEventTest;
import org.openbravo.test.accounting.PostDocumentTest;
import org.openbravo.test.accounting.RecordID2Test;
import org.openbravo.test.authentication.AuthenticationManagerTest;
import org.openbravo.test.cancelandreplace.CancelAndReplaceTest;
import org.openbravo.test.centralbroker.CentralBrokerTest;
import org.openbravo.test.conversionratedoc.ConversionRateDocUniqueTestSuite;
import org.openbravo.test.copyLinesFromOrders.CopyLinesFromOrdersTest;
import org.openbravo.test.costing.TestCosting;
import org.openbravo.test.createlinesfrom.CreateLinesFromTest;
import org.openbravo.test.dal.AdminContextTest;
import org.openbravo.test.dal.ComputedColumnsTest;
import org.openbravo.test.dal.DalConnectionProviderTest;
import org.openbravo.test.dal.DalLockingTest;
import org.openbravo.test.dal.DalPerformanceInventoryLineTest;
import org.openbravo.test.dal.DalPerformanceProductTest;
import org.openbravo.test.dal.DalPerformanceProxyTest;
import org.openbravo.test.dal.DalQueryTest;
import org.openbravo.test.dal.DalStoredProcedureTest;
import org.openbravo.test.dal.DalTest;
import org.openbravo.test.dal.DalUtilTest;
import org.openbravo.test.dal.DynamicEntityTest;
import org.openbravo.test.dal.HiddenUpdateTest;
import org.openbravo.test.dal.ImageTest;
import org.openbravo.test.dal.IssuesTest;
import org.openbravo.test.dal.MappingGenerationTest;
import org.openbravo.test.dal.OBContextTest;
import org.openbravo.test.dal.ReadByNameTest;
import org.openbravo.test.dal.ValidationTest;
import org.openbravo.test.dal.ViewTest;
import org.openbravo.test.datasource.GridExport;
import org.openbravo.test.db.model.functions.ADOrgTreeTest;
import org.openbravo.test.db.model.functions.Ad_isorgincludedTest;
import org.openbravo.test.db.model.functions.CLocationGetIdentifierTest;
import org.openbravo.test.db.model.functions.SqlCallableStatement;
import org.openbravo.test.db.pool.PoolHasNoConnectionsDetection;
import org.openbravo.test.documentsequence.SequenceTestSuite;
import org.openbravo.test.expression.EvaluationTest;
import org.openbravo.test.expression.OBBindingsTest;
import org.openbravo.test.generalsetup.enterprise.organization.ADOrgPersistInfoTestSuite;
import org.openbravo.test.inventoryStatus.InventoryStatusTest;
import org.openbravo.test.matchers.json.JSONMatchersTest;
import org.openbravo.test.materialMgmt.invoiceFromShipment.InvoiceFromShipmentTest;
import org.openbravo.test.materialMgmt.iscompletelyinvoicedshipment.IsCompletelyInvoicedShipment;
import org.openbravo.test.model.ClassLoaderTest;
import org.openbravo.test.model.DBModifiedTest;
import org.openbravo.test.model.IndexesTest;
import org.openbravo.test.model.OneToManyTest;
import org.openbravo.test.model.RuntimeModelTest;
import org.openbravo.test.model.TrlColumnsOraTypeTest;
import org.openbravo.test.model.UniqueConstraintTest;
import org.openbravo.test.modularity.DBPrefixTest;
import org.openbravo.test.modularity.DatasetServiceTest;
import org.openbravo.test.modularity.ExecutionLimitsTest;
import org.openbravo.test.modularity.MergePropertiesTest;
import org.openbravo.test.modularity.TableNameTest;
import org.openbravo.test.preference.PreferenceTest;
import org.openbravo.test.pricelist.PriceListTest;
import org.openbravo.test.process.order.OrderProcessTest;
import org.openbravo.test.productStatus.ProductStatusTest;
import org.openbravo.test.referencedinventory.NestedReferencedInventoryTestSuite;
import org.openbravo.test.referencedinventory.ReferencedInventoryTestSuite;
import org.openbravo.test.reporting.AllJrxmlCompilation;
import org.openbravo.test.reporting.CompiledReportsCacheTest;
import org.openbravo.test.reporting.JasperReportsCompilation;
import org.openbravo.test.reporting.JasperReportsExportTest;
import org.openbravo.test.reporting.ReportingUtilsTest;
import org.openbravo.test.role.inheritance.RoleInheritanceTestSuite;
import org.openbravo.test.security.AccessLevelTest;
import org.openbravo.test.security.AllowedOrganizationsTest;
import org.openbravo.test.security.BOBInClosestOrgTest;
import org.openbravo.test.security.BypassAccessLevelCheck;
import org.openbravo.test.security.CrossOrganizationUI;
import org.openbravo.test.security.CrossOrganizationUICDI;
import org.openbravo.test.security.EntityAccessTest;
import org.openbravo.test.security.OBContextCollectionsTest;
import org.openbravo.test.security.PasswordStrengthCheckerTest;
import org.openbravo.test.security.StandardCrossOrganizationReference;
import org.openbravo.test.security.WritableReadableOrganizationClientTest;
import org.openbravo.test.sendemail.SendEmailTest;
import org.openbravo.test.services.ServicesTest;
import org.openbravo.test.services.ServicesTest2;
import org.openbravo.test.services.ServicesTest3;
import org.openbravo.test.storageBin.StorageBinTest;
import org.openbravo.test.system.CryptoUtilities;
import org.openbravo.test.system.ErrorTextParserIntegrationTest;
import org.openbravo.test.system.ErrorTextParserTest;
import org.openbravo.test.system.ImportEntryBuilderTest;
import org.openbravo.test.system.ImportEntrySizeTest;
import org.openbravo.test.system.Issue29934Test;
import org.openbravo.test.system.JSONSerialization;
import org.openbravo.test.system.Sessions;
import org.openbravo.test.system.SystemServiceTest;
import org.openbravo.test.system.SystemValidatorTest;
import org.openbravo.test.system.TestInfrastructure;
import org.openbravo.test.taxes.ModifyTaxesTest;
import org.openbravo.test.taxes.TaxesTest;
import org.openbravo.test.views.ConfigurableTransactionalFilters;
import org.openbravo.test.views.GCSequenceNumberTests;
import org.openbravo.test.views.SortingFilteringGridConfiguration;
import org.openbravo.test.views.ViewGeneration;
import org.openbravo.test.views.ViewGenerationWithDifferentConfigLevelTest;
import org.openbravo.test.xml.ClientExportImportTest;
import org.openbravo.test.xml.DatasetExportTest;
import org.openbravo.test.xml.DefaultsDataset;
import org.openbravo.test.xml.EntityXMLImportTestBusinessObject;
import org.openbravo.test.xml.EntityXMLImportTestReference;
import org.openbravo.test.xml.EntityXMLImportTestSingle;
import org.openbravo.test.xml.EntityXMLImportTestWarning;
import org.openbravo.test.xml.EntityXMLIssues;
import org.openbravo.test.xml.UniqueConstraintImportTest;
import org.openbravo.userinterface.selectors.test.ExpressionsTest;

/**
 * This test class is called from the ant task run.all.tests by the CI server. It contains all the
 * test cases which are runnable and valid and do not require Tomcat to be running.
 * 
 * Test cases requiring Tomcat (ie. testing web service requests) should be included in
 * {@link WebserviceTestSuite}.
 *
 * @see WebserviceTestSuite
 *
 * @author mtaal
 */

@RunWith(Suite.class)
@Suite.SuiteClasses({

    // authentication
    AuthenticationManagerTest.class, //

    // dal
    DalPerformanceInventoryLineTest.class, //
    DalPerformanceProductTest.class, //
    DalPerformanceProxyTest.class, //
    DalQueryTest.class, //
    DalTest.class, //
    DalLockingTest.class, //
    CentralBrokerTest.class, //
    DalUtilTest.class, //
    IssuesTest.class, //
    DalConnectionProviderTest.class, //
    DynamicEntityTest.class, //
    HiddenUpdateTest.class, //
    MappingGenerationTest.class, //
    ValidationTest.class, //
    OBContextTest.class, //
    DalStoredProcedureTest.class, //
    ReadByNameTest.class, //
    AdminContextTest.class, //
    ViewTest.class, //
    ComputedColumnsTest.class, //
    DatasourceEventObserver.class, //
    PersistanceObserver.class, //
    ImageTest.class, //
    SecurityCheckerTest.class, //

    // database pool
    DataPoolCheckerTest.class, //
    SelectionPoolConfigurationTest.class,

    // expression
    EvaluationTest.class, //
    OBBindingsTest.class, //
    ExpressionsTest.class,

    // matchers
    JSONMatchersTest.class,

    // model
    RuntimeModelTest.class, //
    OneToManyTest.class, //
    UniqueConstraintTest.class, //
    ClassLoaderTest.class, //
    IndexesTest.class, //
    TrlColumnsOraTypeTest.class, //
    ADCSInitialization.class, //
    ADCSTest.class, //
    DBModifiedTest.class, //
    NamingUtilTest.class,

    // modularity
    DatasetServiceTest.class, //
    DBPrefixTest.class, //
    MergePropertiesTest.class, //
    TableNameTest.class,

    // security
    AccessLevelTest.class, //
    AllowedOrganizationsTest.class, //
    EntityAccessTest.class, //
    WritableReadableOrganizationClientTest.class, //
    StandardCrossOrganizationReference.class, //
    BypassAccessLevelCheck.class, //
    CrossOrganizationUI.class, //
    CrossOrganizationUICDI.class, //
    OBContextCollectionsTest.class, //
    PasswordStrengthCheckerTest.class, //
    BOBInClosestOrgTest.class, //

    // system
    SystemServiceTest.class, //
    SystemValidatorTest.class, //
    ErrorTextParserTest.class, //
    ErrorTextParserIntegrationTest.class, //
    TestInfrastructure.class, //
    Issue29934Test.class, //
    ImportEntrySizeTest.class, //
    ImportEntryBuilderTest.class, //
    CryptoUtilities.class, //
    Sessions.class, //
    OBContextTest.class, //
    JSONSerialization.class, //
    PasswordHashing.class, //

    // xml
    ClientExportImportTest.class, //
    EntityXMLImportTestBusinessObject.class, //
    EntityXMLImportTestReference.class, //
    EntityXMLImportTestSingle.class, //
    EntityXMLImportTestWarning.class, //
    EntityXMLIssues.class, //
    UniqueConstraintImportTest.class, //
    DatasetExportTest.class, //
    DefaultsDataset.class, //

    // preferences
    PreferenceTest.class, //
    ClassicSelectorTest.class,

    // Accounting
    RecordID2Test.class, //
    PostDocumentTest.class, //

    // Taxes
    TaxesTest.class, //
    ModifyTaxesTest.class, //

    // Price List
    PriceListTest.class, //

    // costing
    TestCosting.class, //

    // Inventory Status
    InventoryStatusTest.class, //

    // Storage Bin
    StorageBinTest.class, //

    // Characteristics
    CharacteristicEventHandlerTest.class, //
    RelevantCharacteristicPropertyTest.class, //
    RelevantCharacteristicFieldTerminologyChecker.class, //

    // POValidations
    PurchaseDocumentLineTest.class, //

    // PLM Status
    ProductStatusTest.class, //

    // Material Management
    IsCompletelyInvoicedShipment.class, //

    // scheduling
    ProcessSchedulingTest.class, //
    TriggerProviderTest.class, //
    MisfirePolicyTest.class, //
    ProcessBundleSerializationTest.class, //

    // cdi
    CdiInfrastructure.class, //
    ParameterizedCdi.class, //
    ParameterizedCdiWithSkip.class, //
    DalPersistanceEventTest.class, //

    // client application
    ApplicationTest.class, //
    DynamicExpressionParserTest.class, //
    GenerateTypesJSTest.class, //
    MenuTest.class, //
    OBViewFieldHandlerFieldSorterTest.class, //
    MenuTemplateTest.class, //
    DisplayLogicAtServerLevelTest.class, //
    CSSMinifier.class, //

    // client kernel
    FreemarkerTemplateProcessorTest.class, //
    GenerateComponentTest.class, //
    LabelTest.class, //
    CompressionTest.class, //
    RelevantCharacteristicQueryHookTest.class, //
    RelevantCharacteristicAdditionalPropertyResolverTest.class, //

    // service json
    AdvancedQueryBuilderTest.class, //
    JSONWriterToCSVTest.class, //
    JsonUtilsTest.class, //
    OrganizationTimeZoneTest.class, //

    // service datasource
    ModelDataSourceServiceTest.class, //

    // buildValidations and moduleScripts
    ExecutionLimitsTest.class, //

    // role inheritance
    RoleInheritanceTestSuite.class, //

    // attachments and reprintable documents
    ReprintableDocumentTest.class, //

    // db
    SqlCallableStatement.class, //
    PoolHasNoConnectionsDetection.class, //

    // grid configuration
    ViewGenerationWithDifferentConfigLevelTest.class, //
    GCSequenceNumberTests.class, //
    SortingFilteringGridConfiguration.class, //
    ConfigurableTransactionalFilters.class,

    // jasper
    JasperReportsCompilation.class, //
    AllJrxmlCompilation.class, //
    CompiledReportsCacheTest.class, //
    JasperReportsExportTest.class, //
    ReportingUtilsTest.class, //

    // external system
    ExternalSystemProviderTest.class, //
    HttpExternalSystemDataCreationTest.class, //
    OAuth2AccessTokenTest.class,

    // synchronization events
    SynchronizationEventTest.class, //
    EventTriggeringTest.class,

    // Product Services
    ServicesTest.class, //
    ServicesTest2.class, //
    ServicesTest3.class,

    // others
    DocumentNumberGeneration.class, //
    GridExport.class, //
    ViewGeneration.class, //
    OAuth2SignInProviderTest.class, //
    OAuth2TokenAuthenticationManagerTest.class, //
    TimeInvalidatedCacheTest.class, //
    ProductEventHandlerTest.class, //

    // Cancel and Replace Tests
    CancelAndReplaceTest.class, //

    // AD_IsOrgIncluded
    Ad_isorgincludedTest.class, //
    ADOrgTreeTest.class, //

    // C_Location_GetIdentifier
    CLocationGetIdentifierTest.class, //

    // CopyFromOrders refactor
    CopyLinesFromOrdersTest.class,

    // Create Lines From refactor
    CreateLinesFromTest.class,

    // Referenced Inventory
    ReferencedInventoryTestSuite.class,

    // Nested Referenced Inventory
    NestedReferencedInventoryTestSuite.class,

    // Document Sequence
    SequenceTestSuite.class,

    // AD_Org Persist Information
    ADOrgPersistInfoTestSuite.class,

    // Automatic Invoice from Goods Shipment
    InvoiceFromShipmentTest.class,

    // C_Order_Post
    OrderProcessTest.class,

    // Conversion Rate Document Unique constraint refactor
    ConversionRateDocUniqueTestSuite.class,

    // Utilities to manage dates
    OBDateUtilsTest.class,

    // Email Configuration by differents organization
    SendEmailTest.class

})

public class StandaloneTestSuite {
}
