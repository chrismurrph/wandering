mv ./project.clj.prod ./project.clj
rm -rf ./resources/public/wandering/js
lein clean
lein deps
lein cljsbuild once prod
lein uberjar
mv ./project.clj ./project.clj.prod
ln -s ./project.clj.dev ./project.clj
mv ./target/*standalone.jar ./lib
