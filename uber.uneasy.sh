mv ./project.clj.prod ./project.clj
## Theoretically only need to delete from 2nd one here, but good to do both in case I start 'playing'
rm -rf ./resources/public/js
rm -rf ./resources/public/marketing/js
rm -rf ./resources/public/uneasy/js
rm -rf ./resources/public/wandering/js
lein clean
lein deps
lein cljsbuild once uneasy
lein uberjar
mv ./project.clj ./project.clj.prod
ln -s ./project.clj.dev ./project.clj
mv ./target/wandering-1.0.0-SNAPSHOT-standalone.jar ./lib/uneasy-1.0.0-standalone.jar
