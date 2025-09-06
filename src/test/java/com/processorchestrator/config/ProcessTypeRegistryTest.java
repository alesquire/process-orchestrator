package com.processorchestrator.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProcessTypeRegistryTest {

    @Test
    void testRegisterAndGet() {
        ProcessTypeRegistry registry = new ProcessTypeRegistry();
        
        ProcessType processType = new ProcessType("data-processing", "Data processing pipeline");
        registry.register(processType);
        
        ProcessType retrieved = registry.getProcessType("data-processing");
        assertNotNull(retrieved);
        assertEquals("data-processing", retrieved.getName());
        assertEquals("Data processing pipeline", retrieved.getDescription());
    }

    @Test
    void testGetNonExistent() {
        ProcessTypeRegistry registry = new ProcessTypeRegistry();
        
        ProcessType retrieved = registry.getProcessType("non-existent");
        assertNull(retrieved);
    }

    @Test
    void testGetProcessTypeOrThrow() {
        ProcessTypeRegistry registry = new ProcessTypeRegistry();
        
        ProcessType processType = new ProcessType("data-processing", "Data processing pipeline");
        registry.register(processType);
        
        ProcessType retrieved = registry.getProcessTypeOrThrow("data-processing");
        assertNotNull(retrieved);
        assertEquals("data-processing", retrieved.getName());
    }

    @Test
    void testGetProcessTypeOrThrowNonExistent() {
        ProcessTypeRegistry registry = new ProcessTypeRegistry();
        
        assertThrows(IllegalArgumentException.class, () -> {
            registry.getProcessTypeOrThrow("non-existent");
        });
    }

    @Test
    void testGetAllProcessTypes() {
        ProcessTypeRegistry registry = new ProcessTypeRegistry();
        
        ProcessType processType1 = new ProcessType("data-processing", "Data processing pipeline");
        ProcessType processType2 = new ProcessType("deployment", "Deployment pipeline");
        
        registry.register(processType1);
        registry.register(processType2);
        
        var allTypes = registry.getAllProcessTypes();
        assertEquals(2, allTypes.size());
        assertTrue(allTypes.containsKey("data-processing"));
        assertTrue(allTypes.containsKey("deployment"));
    }

    @Test
    void testGetAllProcessTypesReturnsCopy() {
        ProcessTypeRegistry registry = new ProcessTypeRegistry();
        
        ProcessType processType = new ProcessType("data-processing", "Data processing pipeline");
        registry.register(processType);
        
        var allTypes = registry.getAllProcessTypes();
        allTypes.clear(); // This should not affect the registry
        
        assertEquals(1, registry.getAllProcessTypes().size());
    }
}