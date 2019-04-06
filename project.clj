(defproject me.lomin.accounting-piggybank "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/test.check "0.9.0"]
                 [org.clojure/math.combinatorics "0.1.4"]
                 [me.lomin/sayang "0.3.0"]]

  :test-selectors {:default #(not (some #{} (keys %)))
                   :unit    #(not (some #{:model} (keys %)))
                   :focus   :focus
                   :model   :model
                   :all     (constantly true)}

  :aliases {"test"         ["do" ["nsorg" "--replace"] ["cljfmt" "fix"] "test"]
            "test-refresh" ["do" ["nsorg" "--replace"] ["cljfmt" "fix"] "test-refresh"]}

  :middleware [ultra.plugin/middleware]

  :profiles {:dev  {:dependencies [[org.clojure/tools.namespace "0.2.11"]
                                   [progrock "0.1.2"]
                                   [orchestra "2018.08.19-1"]]
                    :plugins      [[com.jakemccrary/lein-test-refresh "0.23.0"]
                                   [lein-kibit "0.1.6"]
                                   [lein-nvd "0.5.4"]
                                   [jonase/eastwood "0.3.5"]
                                   [lein-cljfmt "0.5.6"]
                                   [lein-ancient "0.6.15"]
                                   [lein-nsorg "0.2.0"]
                                   [venantius/ultra "0.5.4"]]
                    :jvm-opts ["-Dme.lomin.sayang.*activate*=true"]
                    }
             :test {:jvm-opts       ["-Xms2048m" "-Xmx2048m" ~(str "-Djava.io.tmpdir=" (System/getenv "HOME"))]
                    :resource-paths ["test-resources"]}})
