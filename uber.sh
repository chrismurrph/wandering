mv ./project.clj.prod ./project.clj
## Theoretically only need to delete from 2nd one here, but good to do both in case I start 'playing'
rm -rf ./resources/public/js
rm -rf ./resources/public/wandering/js
lein clean
lein deps
lein cljsbuild once prod
lein uberjar
mv ./project.clj ./project.clj.prod
ln -s ./project.clj.dev ./project.clj
mv ./target/*standalone.jar ./lib
