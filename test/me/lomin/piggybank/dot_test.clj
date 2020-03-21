(ns me.lomin.piggybank.dot-test
  (:require [clojure.test :refer :all]
            [instaparse.core :as insta]))
(def grammar
  "
graph = ['strict'] ('digraph' | 'graph') id <'{'> stmt-list <'}'>
stmt-list = (stmt <[';']>)*
                   stmt = attr-stmt | node-stmt | edge-stmt | subgraph | id <'='> id
                   attr-stmt = (graph | 'node' | 'edge') attr-list
                   attr-list = <'['> [a-list] <']'> [attr-list]
                   a-list = id <'='> (id | label) <[',']> [a-list]
                   label = <'\"'> #'[^\"]+' <'\"'>
                   node-stmt = node-id [attr-list]
                   node-id = id [port]
                   port = port-location [port-angle] | port-angle [port-location]
                   port-location = ':' id | ':' '(' id ',' id ')'
                   port-angle = '@' id
                   edge-stmt = (node-id | subgraph) edgeRHS [attr-list]
                   edgeRHS = <edgeop> (node-id | subgraph) [edgeRHS]
                   edgeop = '->'
                   id = [<whitespace>] #'[-.\"0-9a-zA-Z]+'[<whitespace>]
                   word = #'[a-zA-Z]+'
                   number = #'[0-9]+'
                   special-chars = '\"' | '.'
                   whitespace = #'\\s+'
                   subgraph = [subgraph id] '{' stmt-list '}' | subgraph id
")

(def example
  "digraph G {
             edge [label=0];
             graph [ranksep=0];
             -1974429183 [shape=record, label=\"...\"];
             -834983646 [shape=record, label=\"...\"];
             53289931 [shape=record, label=\"...\"];
             -978615061 [shape=record, label=\"...\"];
             -880793513 [shape=record, label=\"...\"];
             start [shape=record, label=\"start\"];
             -1427502197 [shape=record, label=\"...\"];
             127232727 [shape=record, label=\"...\"];
             -6142460 [shape=record, label=\"...\"];
             -1974429183 ->  -834983646[label=\"[:-1€ 1]\"];
             -1974429183 ->  -6142460[label=\"[:+1€ 1]\"];
             -1974429183 ->  -880793513[label=\"[:ar 0]\"];
             start ->  -1974429183[label=\"[:+1€ 0]\"];
             start ->  127232727[label=\"[:-1€ 0]\"];
             127232727 ->  -978615061[label=\"[:ar 0]\"];
             127232727 ->  -1427502197[label=\"[:+1€ 1]\"];
             127232727 ->  53289931[label=\"[:-1€ 1]\"];}")

(def parsed-example
  [:graph
   "digraph"
   [:id "G"]
   [:stmt-list
    [:stmt [:node-stmt [:node-id [:id "edge"]] [:attr-list [:a-list [:id "label"] [:id "0"]]]]]
    [:stmt [:node-stmt [:node-id [:id "graph"]] [:attr-list [:a-list [:id "ranksep"] [:id "0"]]]]]
    [:stmt
     [:node-stmt
      [:node-id [:id "-1974429183"]]
      [:attr-list [:a-list [:id "shape"] [:id "record"] [:a-list [:id "label"] [:label "..."]]]]]]
    [:stmt
     [:node-stmt
      [:node-id [:id "-834983646"]]
      [:attr-list [:a-list [:id "shape"] [:id "record"] [:a-list [:id "label"] [:label "..."]]]]]]
    [:stmt
     [:node-stmt
      [:node-id [:id "53289931"]]
      [:attr-list [:a-list [:id "shape"] [:id "record"] [:a-list [:id "label"] [:label "..."]]]]]]
    [:stmt
     [:node-stmt
      [:node-id [:id "-978615061"]]
      [:attr-list [:a-list [:id "shape"] [:id "record"] [:a-list [:id "label"] [:label "..."]]]]]]
    [:stmt
     [:node-stmt
      [:node-id [:id "-880793513"]]
      [:attr-list [:a-list [:id "shape"] [:id "record"] [:a-list [:id "label"] [:label "..."]]]]]]
    [:stmt
     [:node-stmt
      [:node-id [:id "start"]]
      [:attr-list [:a-list [:id "shape"] [:id "record"] [:a-list [:id "label"] [:label "start"]]]]]]
    [:stmt
     [:node-stmt
      [:node-id [:id "-1427502197"]]
      [:attr-list [:a-list [:id "shape"] [:id "record"] [:a-list [:id "label"] [:label "..."]]]]]]
    [:stmt
     [:node-stmt
      [:node-id [:id "127232727"]]
      [:attr-list [:a-list [:id "shape"] [:id "record"] [:a-list [:id "label"] [:label "..."]]]]]]
    [:stmt
     [:node-stmt
      [:node-id [:id "-6142460"]]
      [:attr-list [:a-list [:id "shape"] [:id "record"] [:a-list [:id "label"] [:label "..."]]]]]]
    [:stmt
     [:edge-stmt
      [:node-id [:id "-1974429183"]]
      [:edgeRHS [:node-id [:id "-834983646"]]]
      [:attr-list [:a-list [:id "label"] [:label "[:-1€ 1]"]]]]]
    [:stmt
     [:edge-stmt
      [:node-id [:id "-1974429183"]]
      [:edgeRHS [:node-id [:id "-6142460"]]]
      [:attr-list [:a-list [:id "label"] [:label "[:+1€ 1]"]]]]]
    [:stmt
     [:edge-stmt
      [:node-id [:id "-1974429183"]]
      [:edgeRHS [:node-id [:id "-880793513"]]]
      [:attr-list [:a-list [:id "label"] [:label "[:ar 0]"]]]]]
    [:stmt
     [:edge-stmt
      [:node-id [:id "start"]]
      [:edgeRHS [:node-id [:id "-1974429183"]]]
      [:attr-list [:a-list [:id "label"] [:label "[:+1€ 0]"]]]]]
    [:stmt
     [:edge-stmt
      [:node-id [:id "start"]]
      [:edgeRHS [:node-id [:id "127232727"]]]
      [:attr-list [:a-list [:id "label"] [:label "[:-1€ 0]"]]]]]
    [:stmt
     [:edge-stmt
      [:node-id [:id "127232727"]]
      [:edgeRHS [:node-id [:id "-978615061"]]]
      [:attr-list [:a-list [:id "label"] [:label "[:ar 0]"]]]]]
    [:stmt
     [:edge-stmt
      [:node-id [:id "127232727"]]
      [:edgeRHS [:node-id [:id "-1427502197"]]]
      [:attr-list [:a-list [:id "label"] [:label "[:+1€ 1]"]]]]]
    [:stmt
     [:edge-stmt
      [:node-id [:id "127232727"]]
      [:edgeRHS [:node-id [:id "53289931"]]]
      [:attr-list [:a-list [:id "label"] [:label "[:-1€ 1]"]]]]]]])

(def parser (insta/parser grammar))

(comment
  (parser example))