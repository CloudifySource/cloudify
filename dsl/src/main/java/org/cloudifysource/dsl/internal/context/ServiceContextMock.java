package org.cloudifysource.dsl.internal.context;

import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;
import org.cloudifysource.dsl.context.Service;
import org.cloudifysource.dsl.context.ServiceContext;
import org.cloudifysource.dsl.context.blockstorage.StorageFacade;
import org.cloudifysource.dsl.context.kvstorage.*;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.*;

import java.lang.management.ManagementFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * User: Sagi Bernstein
 * Date: 11/04/13
 * Time: 18:07
 */
public class ServiceContextMock implements ServiceContext {
    AttributesFacade attributes = new MockAttributesFacade();

    @Override
    public int getInstanceId() {
        return Integer.getInteger("debug.local.instanceId", 1);
    }

    @Override
    public Service waitForService(String name, int timeout, TimeUnit unit) {
        //maybe throw new IllegalStateException("not supported in local debug mode");
        return null;
    }

    @Override
    public String getServiceDirectory() {
        return System.getProperty("debug.local.serviceDir", System.getProperty("user.dir"));
    }

    @Override
    public String getServiceName() {
        return System.getProperty("debug.local.serviceName", "serviceName");
    }

    @Override
    public String getApplicationName() {
        return System.getProperty("debug.local.applicationName", "applicationName");
    }

    @Override
    public AttributesFacade getAttributes() {
        return attributes;
    }


    //returns external pid
    @Override
    public long getExternalProcessId() {
        return Long.getLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
    }

    //returns false since this is local debug
    @Override
    public boolean isLocalCloud() {
        return false;
    }

    @Override
    public String getPublicAddress() {
        return System.getProperty("debug.local.publicAddress", "0.0.0.0");
    }

    @Override
    public String getPrivateAddress() {
        return System.getProperty("debug.local.privateAddress", "0.0.0.0");
    }

    @Override
    public String getImageID() {
        return System.getProperty("debug.local.imageId", "imageId");
    }

    @Override
    public String getHardwareID() {
        return System.getProperty("debug.local.privateAddress", "hardwareId");
    }

    @Override
    public String getCloudTemplateName() {
        return System.getProperty("debug.local.cloudTemplateName", "cloudTemplateName");
    }

    @Override
    public String getMachineID() {
        return System.getProperty("debug.local.machineId", "machineId");
    }

    @Override
    public String getLocationId() {
        return System.getProperty("debug.local.locationId", "locationId");
    }

    @Override
    public StorageFacade getStorage() {
        //maybe throw new IllegalStateException("not supported in local debug mode");
        return null;
    }

    @Override
    public boolean isPrivileged() {
        return false;
    }

    @Override
    public String getBindAddress() {
        return System.getProperty("debug.local.bindAddress", "0.0.0.0");
    }

    class MockAttributesFacade extends GroovyObjectSupport implements AttributesFacade {

        private final MockApplicationAttributesAccessor applicationAttributesAccessor;
        private final MockServiceAttributesAccessor serviceAttributesAccessor;
        private final MockGlobalAttributesAccessor globalAttributesAccessor;
        private final MockInstanceAttributesAccessor instanceAttributesAccessor;

        public MockAttributesFacade(){
            this.applicationAttributesAccessor =
                    new MockApplicationAttributesAccessor();
            this.serviceAttributesAccessor =
                    new MockServiceAttributesAccessor();
            this.globalAttributesAccessor =
                    new MockGlobalAttributesAccessor();
            this.instanceAttributesAccessor =
                    new MockInstanceAttributesAccessor();
        }

        @Override
        public MockApplicationAttributesAccessor getThisApplication() {

            return this.applicationAttributesAccessor;
        }

        @Override
        public MockGlobalAttributesAccessor getGlobal() {
            return this.globalAttributesAccessor;
        }

        @Override
        public MockServiceAttributesAccessor getThisService() {
            return this.serviceAttributesAccessor;
        }

        @Override
        public MockInstanceAttributesAccessor getThisInstance() {
            return this.instanceAttributesAccessor;
        }

        @Override
        public Object getProperty(final String property) {
            try {
                return super.getProperty(property);
            } catch (final MissingPropertyException e) {
                return getThisService().getProperty(property);
            }
        }

        private class MockApplicationAttributesAccessor extends ApplicationAttributesAccessor {
            private ConcurrentMap<Object,Object> attributes;

            public MockApplicationAttributesAccessor() {
                super(null, null);
                attributes = new ConcurrentHashMap<Object,Object>();
            }


            @Override
            protected ApplicationCloudifyAttribute prepareAttributeTemplate() {
                return null;
            }

            @Override
            public void setProperty(final String name, final Object value) {
                try {
                    super.setProperty(name, value);
                } catch (final MissingPropertyException e) {
                    put(name, value);
                }
            }

            private Object put(final String key, final Object value) {
                return attributes.replace(key, value);
            }


            /********
             * Groovy element remover.
             * @param key element key.
             * @return the element.
             */
            @Override
            public Object remove(final String key) {
                return attributes.remove(key);
            }

            /*********
             * Clears the attributes.
             */
            @Override
            public void clear() {
                attributes.clear();
            }

            /*********
             * Groovy element accessor.
             * @param key the element key.
             * @return the element value.
             */
            @Override
            public Object get(final String key) {
                return attributes.get(key);
            }

            /**************
             * check if attribute with specified key exists.
             * @param key the element key.
             * @return true if the an element with this key exists, false otherwise.
             */
            @Override
            public boolean containsKey(final String key) {
                return attributes.containsKey(key);
            }

        }

        private class MockServiceAttributesAccessor extends ServiceAttributesAccessor {
            private ConcurrentMap<Object,Object> attributes;
            public MockServiceAttributesAccessor() {
                super(null, null, null, null);
                attributes = new ConcurrentHashMap<Object,Object>();
            }
            @Override
            protected ServiceCloudifyAttribute prepareAttributeTemplate() {
                return null;
            }
            @Override
            public void setProperty(final String name, final Object value) {
                try {
                    super.setProperty(name, value);
                } catch (final MissingPropertyException e) {
                    put(name, value);
                }
            }
            private Object put(final String key, final Object value) {
                return attributes.replace(key, value);
            }
            @Override
            public Object remove(final String key) {
                return attributes.remove(key);
            }
            @Override
            public void clear() {
                attributes.clear();
            }
            @Override
            public Object get(final String key) {
                return attributes.get(key);
            }
            @Override
            public boolean containsKey(final String key) {
                return attributes.containsKey(key);
            }
        }

        private class MockInstanceAttributesAccessor extends InstanceAttributesAccessor {
            private ConcurrentMap<Object,Object> attributes;
            public MockInstanceAttributesAccessor() {
                super(null, null, null, 0);
                attributes = new ConcurrentHashMap<Object,Object>();
            }
            @Override
            protected InstanceCloudifyAttribute prepareAttributeTemplate() {
                return null;
            }
            @Override
            public void setProperty(final String name, final Object value) {
                try {
                    super.setProperty(name, value);
                } catch (final MissingPropertyException e) {
                    put(name, value);
                }
            }
            private Object put(final String key, final Object value) {
                return attributes.replace(key, value);
            }
            @Override
            public Object remove(final String key) {
                return attributes.remove(key);
            }
            @Override
            public void clear() {
                attributes.clear();
            }
            @Override
            public Object get(final String key) {
                return attributes.get(key);
            }
            @Override
            public boolean containsKey(final String key) {
                return attributes.containsKey(key);
            }
        }
        private class MockGlobalAttributesAccessor extends GlobalAttributesAccessor {
            private ConcurrentMap<Object,Object> attributes;
            public MockGlobalAttributesAccessor() {
                super(null);
                attributes = new ConcurrentHashMap<Object,Object>();
            }
            @Override
            protected GlobalCloudifyAttribute prepareAttributeTemplate() {
                return null;
            }
            @Override
            public void setProperty(final String name, final Object value) {
                try {
                    super.setProperty(name, value);
                } catch (final MissingPropertyException e) {
                    put(name, value);
                }
            }
            private Object put(final String key, final Object value) {
                return attributes.replace(key, value);
            }
            @Override
            public Object remove(final String key) {
                return attributes.remove(key);
            }
            @Override
            public void clear() {
                attributes.clear();
            }
            @Override
            public Object get(final String key) {
                return attributes.get(key);
            }
            @Override
            public boolean containsKey(final String key) {
                return attributes.containsKey(key);
            }
        }
    }
}
