Add main class if you recreate a module-info.class :

jar --main-class fr.gaellalire.vestige.core.JPMSVestige --update --file target/vestige.core*.jar; \
pushd src/main/resources/ && jar xf ../../../target/vestige.core*.jar module-info.class; popd
