package fr.gaellalire.vestige.core;

public interface VestigeClassLoaderConfiguration {

    VestigeClassLoaderConfiguration THIS_PARENT_SEARCHED = new VestigeClassLoaderConfiguration() {

        @Override
        public boolean isParentSearched() {
            return true;
        }

        @Override
        public VestigeClassLoader<?> getVestigeClassLoader() {
            return null;
        }

        @Override
        public String toString() {
            return "THIS_PARENT_SEARCHED";
        }
    };

    VestigeClassLoaderConfiguration THIS_PARENT_UNSEARCHED = new VestigeClassLoaderConfiguration() {

        @Override
        public boolean isParentSearched() {
            return false;
        }

        @Override
        public VestigeClassLoader<?> getVestigeClassLoader() {
            return null;
        }

        @Override
        public String toString() {
            return "THIS_PARENT_UNSEARCHED";
        }
    };

    VestigeClassLoader<?> getVestigeClassLoader();

    boolean isParentSearched();

}
