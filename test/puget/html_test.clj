(ns puget.html-test
  (:require
    [clojure.test :refer :all]
    (puget
     [html :as html]
     [printer :as printer])))


(def test-color-scheme
  {:delimiter [:green]
   :tag       [:bold :white]
   :nil       [:black]
   :boolean   [:cyan]
   :number    [:red]
   :string    [:magenta :underline]
   :character [:yello]
   :keyword   [:cyan]
   :symbol    nil
   :function-symbol [:bold :blue]
   :class-delimiter [:blue]
   :class-name      [:bold :blue]})


(deftest style-test
  (is (= "style=\"font-weight:bold;text-decoration:underline;color:red\""
         (html/style [:bold :underline :red]))))


(deftest html-test
  (let [test-data {:a 1 :b 2 "c" 3.0 'd [1 2 3] \e #inst "2001"}
        inline-ref
        (str
         "<span style=\"color:green\">{</span>"
         "<span style=\"\">\\e</span> "
         "<span style=\"font-weight:bold;color:white\">#inst</span> "
         "<span style=\"color:magenta;text-decoration:underline\">"
         "&quot;2001-01-01T00:00:00.000-00:00&quot;</span>, "
         "<span style=\"color:magenta;text-decoration:underline\">&quot;c&quot;</span> "
         "<span style=\"color:red\">3.0</span>, "
         "<span style=\"color:cyan\">:a</span> "
         "<span style=\"color:red\">1</span>, "
         "<span style=\"color:cyan\">:b</span> "
         "<span style=\"color:red\">2</span>, d "
         "<span style=\"color:green\">[</span>"
         "<span style=\"color:red\">1</span> "
         "<span style=\"color:red\">2</span> "
         "<span style=\"color:red\">3</span>"
         "<span style=\"color:green\">]</span>"
         "<span style=\"color:green\">}</span>")
        classes-ref
        (str
         "<span class=\"delimiter\">{</span>"
         "<span class=\"character\">\\e</span> "
         "<span class=\"tag\">#inst</span> "
         "<span class=\"string\">&quot;2001-01-01T00:00:00.000-00:00&quot;</span>, "
         "<span class=\"string\">&quot;c&quot;</span> "
         "<span class=\"number\">3.0</span>, "
         "<span class=\"keyword\">:a</span> "
         "<span class=\"number\">1</span>, "
         "<span class=\"keyword\">:b</span> "
         "<span class=\"number\">2</span>, "
         "<span class=\"symbol\">d</span> "
         "<span class=\"delimiter\">[</span>"
         "<span class=\"number\">1</span> "
         "<span class=\"number\">2</span> "
         "<span class=\"number\">3</span>"
         "<span class=\"delimiter\">]</span>"
         "<span class=\"delimiter\">}</span>")]
    (is (= inline-ref
           (printer/cprint-str test-data {:color-markup :html-inline
                                          :color-scheme test-color-scheme})))
    (is (= classes-ref
           (printer/cprint-str test-data {:color-markup :html-classes})))))
