/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.zosprogram.internal;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import dev.galasa.artifact.IBundleResources;
import dev.galasa.artifact.TestBundleResourceException;
import dev.galasa.zos.IZosImage;
import dev.galasa.zos.ZosManagerException;
import dev.galasa.zos.spi.IZosManagerSpi;
import dev.galasa.zosbatch.IZosBatch;
import dev.galasa.zosbatch.IZosBatchJob;
import dev.galasa.zosbatch.IZosBatchJobname;
import dev.galasa.zosbatch.ZosBatchException;
import dev.galasa.zosbatch.spi.IZosBatchSpi;
import dev.galasa.zosfile.IZosDataset;
import dev.galasa.zosfile.IZosFileHandler;
import dev.galasa.zosfile.ZosFileManagerException;
import dev.galasa.zosfile.spi.IZosFileSpi;
import dev.galasa.zosprogram.ZosProgram.Language;
import dev.galasa.zosprogram.ZosProgramException;
import dev.galasa.zosprogram.ZosProgramManagerException;
import dev.galasa.zosprogram.internal.properties.CICSDatasetPrefix;
import dev.galasa.zosprogram.internal.properties.LanguageEnvironmentDatasetPrefix;
import dev.galasa.zosprogram.internal.properties.ProgramLanguageCompileSyslibs;
import dev.galasa.zosprogram.internal.properties.ProgramLanguageDatasetPrefix;
import dev.galasa.zosprogram.internal.properties.ProgramLanguageLinkSyslibs;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ProgramLanguageDatasetPrefix.class, LanguageEnvironmentDatasetPrefix.class, ProgramLanguageCompileSyslibs.class, ProgramLanguageLinkSyslibs.class, CICSDatasetPrefix.class})
public class TestZosProgramImpl {
    
    private ZosProgramImpl zosProgram;
    
    private ZosProgramImpl zosProgramSpy;

    @Mock
    private IZosImage zosImageMock;
    
    @Mock
    private IZosManagerSpi zosManagerMock;
    
    @Mock
    private IZosFileSpi zosFileManagerMock;
    
    @Mock
    private IZosFileHandler zosFileHandlerMock;

    @Mock
    private IZosDataset loadlibMock;

    @Mock
    private IZosBatchJob zosBatchJobMock;
    
    @Mock
    private Field fieldMock;
    
    @Mock
    private IBundleResources testBundleResourcesMock;

    @Mock
    private IBundleResources bundleResourcesMock;

    @Mock
    private ZosProgramManagerImpl zosBatchManagerMock;

    @Mock
    private IZosBatch zosBatchMock;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    
    private static final String IMAGE_TAG = "TAG";

    private static final String NAME = "NAME";

    private static final String LOCATION = "LOCATION";

    private static final Language LANGUAGE = Language.COBOL;
    
    private static final boolean CICS = true;

    private static final String LOADLIB_NAME = "LOAD.LIBRARY";

    private static final String SOURCE = "SOURCE";    

    private static final String EXCEPTION = "EXCEPTION";

    private static final String JCL = "JCL";

    @Before
    public void setup() throws Exception {
        ZosProgramManagerImpl.setZosManager(zosManagerMock);
        Mockito.when(zosManagerMock.getImageForTag(Mockito.any())).thenReturn(zosImageMock);
        ZosProgramManagerImpl.setZosFile(zosFileManagerMock);
        Mockito.when(zosFileManagerMock.getZosFileHandler()).thenReturn(zosFileHandlerMock);
        Mockito.when(zosFileHandlerMock.newDataset(Mockito.any(), Mockito.any())).thenReturn(loadlibMock);
        Mockito.when(loadlibMock.toString()).thenReturn(LOADLIB_NAME);
        
        zosProgram = new ZosProgramImpl(fieldMock, IMAGE_TAG, NAME, LOCATION, LANGUAGE, CICS, LOADLIB_NAME, true);
        zosProgramSpy = Mockito.spy(zosProgram);
    }
    
    @Test
    public void testConstructors() throws ZosManagerException {
        Assert.assertEquals("Error in constructor", loadlibMock, zosProgramSpy.getLoadlib());
        
        ZosProgramImpl zosProgram2 = new ZosProgramImpl(zosImageMock, NAME, SOURCE, LANGUAGE, CICS, LOADLIB_NAME);
        Assert.assertEquals("Error in constructor", loadlibMock, zosProgram2.getLoadlib());

        Mockito.when(zosManagerMock.getImageForTag(Mockito.any())).thenThrow(new ZosManagerException(EXCEPTION));
        exceptionRule.expect(ZosProgramException.class);
        exceptionRule.expectMessage(EXCEPTION);
        new ZosProgramImpl(fieldMock, IMAGE_TAG, NAME, LOCATION, LANGUAGE, CICS, LOADLIB_NAME, true);
    }
    
    @Test
    public void testGetters() {
        Assert.assertEquals("Error in getField()", fieldMock, zosProgramSpy.getField());
        Assert.assertEquals("Error in getName()", NAME, zosProgramSpy.getName());
        Assert.assertEquals("Error in getLocation()", LOCATION, zosProgramSpy.getLocation());
        Assert.assertEquals("Error in getLanguage()", LANGUAGE, zosProgramSpy.getLanguage());
        Assert.assertTrue("Error in isCics()", zosProgramSpy.isCics());
        Assert.assertEquals("Error in getLoadlib()", loadlibMock, zosProgramSpy.getLoadlib());
        Assert.assertEquals("Error in getImage()", zosImageMock, zosProgramSpy.getImage());
        Assert.assertEquals("Error in getCompile()", true, zosProgramSpy.getCompile());
        zosProgramSpy.setCompileJob(zosBatchJobMock);
        Assert.assertEquals("Error in getCompileJob()", zosBatchJobMock, zosProgramSpy.getCompileJob());
    }
    
    @Test
    public void testGetProgramSource() throws ZosProgramException {
        Whitebox.setInternalState(zosProgramSpy, "programSource", SOURCE);
        Assert.assertEquals("Error in getProgramSource()", SOURCE, zosProgramSpy.getProgramSource());

        Whitebox.setInternalState(zosProgramSpy, "programSource", (String) null);
        PowerMockito.doNothing().when(zosProgramSpy).loadProgramSource();
        Assert.assertNull("Error in getProgramSource()", zosProgramSpy.getProgramSource());
    }
    
    @Test
    public void testToString() throws ZosProgramException {
        String result = "name=" + NAME + ", location=" + LOCATION + ", language=" + LANGUAGE +  ", loadlib=" + loadlibMock + ", image=" + zosImageMock;
        Assert.assertEquals("Error in toString()", result, zosProgramSpy.toString());
    }
    
    @Test
    public void testSetLoadlib() throws ZosProgramException, ZosFileManagerException {
        zosProgramSpy.setLoadlib(LOADLIB_NAME);
        Assert.assertEquals("Error in setLoadlib()", loadlibMock, zosProgramSpy.getLoadlib());
        
        zosProgramSpy.setLoadlib(loadlibMock);
        Assert.assertEquals("Error in setLoadlib()", loadlibMock, zosProgramSpy.getLoadlib());
        
        zosProgramSpy.setLoadlib(null);
        Assert.assertEquals("Error in setLoadlib()", loadlibMock, zosProgramSpy.getLoadlib());
        
        Mockito.when(zosFileManagerMock.getZosFileHandler()).thenThrow(new ZosFileManagerException());
        exceptionRule.expect(ZosProgramException.class);
        exceptionRule.expectMessage("Unable to instantiate loadlib data set object");
        zosProgramSpy.setLoadlib(LOADLIB_NAME);
    }
    
    @Test
    public void testLoadProgramSource() throws ZosProgramException, TestBundleResourceException, IOException {
        ZosProgramManagerImpl.setTestBundleResources(testBundleResourcesMock);
        Mockito.when(testBundleResourcesMock.retrieveFileAsString(Mockito.any())).thenReturn(SOURCE);
        zosProgramSpy.loadProgramSource();
        Assert.assertEquals("Error in loadProgramSource()", SOURCE, zosProgramSpy.getProgramSource());
        
        Whitebox.setInternalState(zosProgramSpy, "programSource", (String) null);
        Whitebox.setInternalState(zosProgramSpy, "location", LOCATION + ".cbl");
        Assert.assertEquals("Error in loadProgramSource()", SOURCE, zosProgramSpy.getProgramSource());
        
        Whitebox.setInternalState(zosProgramSpy, "programSource", (String) null);
        Whitebox.setInternalState(zosProgramSpy, "location", (String) null);
        Assert.assertEquals("Error in loadProgramSource()", SOURCE, zosProgramSpy.getProgramSource());

        Mockito.when(testBundleResourcesMock.retrieveFileAsString(Mockito.any())).thenThrow(new IOException());
        exceptionRule.expect(ZosProgramException.class);
        exceptionRule.expectMessage("Problem loading program source");
        zosProgramSpy.loadProgramSource();
    }
    
    @Test
    public void testCompile() throws ZosProgramManagerException, IOException, ZosBatchException {
        Mockito.when(zosProgramSpy.getLanguage()).thenReturn(Language.ASSEMBLER);
        Mockito.when(zosProgramSpy.getLoadlib()).thenReturn(loadlibMock);
        Mockito.when(zosProgramSpy.getImage()).thenReturn(zosImageMock);
        Mockito.when(zosProgramSpy.getName()).thenReturn(NAME);
        PowerMockito.mockStatic(ProgramLanguageDatasetPrefix.class);
        Mockito.when(ProgramLanguageDatasetPrefix.get(Mockito.any(), Mockito.any())).thenReturn(Arrays.asList());
        PowerMockito.mockStatic(LanguageEnvironmentDatasetPrefix.class);
        Mockito.when(LanguageEnvironmentDatasetPrefix.get(Mockito.any())).thenReturn(Arrays.asList());
        PowerMockito.mockStatic(ProgramLanguageCompileSyslibs.class);
        Mockito.when(ProgramLanguageCompileSyslibs.get(Mockito.any(), Mockito.any())).thenReturn(Arrays.asList());
        PowerMockito.mockStatic(ProgramLanguageLinkSyslibs.class);
        Mockito.when(ProgramLanguageLinkSyslibs.get(Mockito.any(), Mockito.any())).thenReturn(Arrays.asList());
        PowerMockito.mockStatic(CICSDatasetPrefix.class);
        Mockito.when(CICSDatasetPrefix.get(Mockito.any())).thenReturn(Arrays.asList());
        Mockito.when(bundleResourcesMock.streamAsString(Mockito.any())).thenReturn(JCL);
        ZosProgramManagerImpl.setManagerBundleResources(bundleResourcesMock);
        ZosProgramManagerImpl.setTestBundleResources(bundleResourcesMock);
        IZosBatchSpi zosBatchSpiMock = Mockito.mock(IZosBatchSpi.class);
        ZosProgramManagerImpl.setZosBatch(zosBatchSpiMock);
        Mockito.when(ZosProgramManagerImpl.getZosBatch(Mockito.any())).thenReturn(zosBatchMock);
        Mockito.when(zosBatchJobMock.getRetcode()).thenReturn("CC 0000");
        IZosBatchJobname zosJobnameMock = Mockito.mock(IZosBatchJobname.class);
        Mockito.when(zosJobnameMock.getName()).thenReturn("JOBNAME");
        Mockito.when(zosBatchJobMock.getJobname()).thenReturn(zosJobnameMock);
        Mockito.when(zosBatchJobMock.getJobId()).thenReturn("JOBID");
        Mockito.when(zosBatchMock.submitJob(Mockito.any(), Mockito.isNull())).thenReturn(zosBatchJobMock);
        Assert.assertEquals("Error in compile() method", zosProgramSpy, zosProgramSpy.compile());

        Mockito.when(zosProgramSpy.getLanguage()).thenReturn(Language.COBOL);
        Assert.assertEquals("Error in compile() method", zosProgramSpy, zosProgramSpy.compile());

        Mockito.when(zosProgramSpy.getLanguage()).thenReturn(Language.C);
        Assert.assertEquals("Error in compile() method", zosProgramSpy, zosProgramSpy.compile());

        Mockito.when(zosProgramSpy.getLanguage()).thenReturn(Language.PL1);
        Assert.assertEquals("Error in compile() method", zosProgramSpy, zosProgramSpy.compile());

        Mockito.when(zosProgramSpy.getLanguage()).thenReturn(Language.INVALID);
        exceptionRule.expect(ZosProgramManagerException.class);
        exceptionRule.expectMessage("Invalid program language: " + Language.INVALID);
        zosProgramSpy.compile();
    }
    
    @Test
    public void testLogForField() {
        Whitebox.setInternalState(fieldMock, "name", NAME);
        Assert.assertEquals("Error in logForField()", " for field \"" + NAME + "\"", zosProgramSpy.logForField());
        
        Whitebox.setInternalState(zosProgramSpy, "field", (Field) null);
        Assert.assertEquals("Error in logForField()", "", zosProgramSpy.logForField());
    }
}