(defproject me.lomin.piggybank "0.1.0-SNAPSHOT"
  :description "This projects describes a very simple way to model and check the design of a distributed system."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/test.check "0.9.0"]
                 [org.clojure/math.combinatorics "0.1.4"]
                 [progrock "0.1.2"]
                 [ubergraph "0.6.0"]
                 [org.clojure/clojurescript "1.10.520"]
                 [org.clojure/data.json "0.2.6"]
                 [me.lomin/sayang "0.3.0"]]

  :test-selectors {:default    #(not (some #{:slow-model} (keys %)))
                   :unit       #(not (some #{:slow-model :model} (keys %)))
                   :focus      :focus
                   :model      :model
                   :slow-model :slow-model
                   :all        (constantly true)}

  :aliases {"test"         ["do" ["nsorg" "--replace"] ["cljfmt" "fix"] "test"]
            "test-refresh" ["do" ["nsorg" "--replace"] ["cljfmt" "fix"] "test-refresh"]}

  :profiles {:dev  {:dependencies [[org.clojure/tools.namespace "0.2.11"]
                                   [orchestra "2018.08.19-1"]]
                    :plugins      [[com.jakemccrary/lein-test-refresh "0.24.1"]
                                   [lein-kibit "0.1.6"]
                                   [lein-nvd "0.5.4"]
                                   [jonase/eastwood "0.3.5"]
                                   [lein-cljfmt "0.5.6"]
                                   [lein-ancient "0.6.15"]
                                   [lein-nsorg "0.2.0"]]
                    :jvm-opts     ["-Dme.lomin.sayang.*activate*=true"]
                    }
             :test {:jvm-opts       ["-Xms2048m" "-Xmx2048m" ~(str "-Djava.io.tmpdir=" (System/getenv "HOME"))]
                    :resource-paths ["test-resources"]}})
