(ns puget.color.html-test
  (:require
    [clojure.test :refer :all]
    [puget.color.html :as html]
    [puget.printer :as printer]))


(def test-color-scheme
  {:delimiter [:green]
   :tag       [:bold :white]
   :nil       [:black]
   :boolean   [:cyan]
   :number    [:red]
   :string    [:magenta :underline]
   :character [:darkorange]
   :keyword   [:cyan]
   :symbol    nil
   :function-symbol [:bold :blue]
   :class-delimiter [:blue]
   :class-name      [:bold :blue]})


(def inline-color
  {:print-color true
   :color-markup :html-inline})


(def classes-color
  {:print-color true
   :color-markup :html-classes})


(deftest style-test
  (is (= "style=\"font-weight:bold;text-decoration:underline;color:red\""
         (html/style [:bold :underline :red]))))


(deftest html-test
  (let [test-data {:a 1 :b 2 "c" 3.0 'd [1 2 3] \e #inst "2001"}
        inline-ref
        (str
          "<span style=\"color:green\">{</span>"
          "<span style=\"color:darkorange\">\\e</span> "
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
           (printer/cprint-str
             test-data
             {:color-markup :html-inline
              :color-scheme test-color-scheme
              :print-handlers printer/java-handlers})))
    (is (= classes-ref
           (printer/cprint-str
             test-data
             {:color-markup :html-classes
              :print-handlers printer/java-handlers}))))
  (testing "color-text"
    (testing "no color markup"
      (is (= ":inline>"
             (printer/color-text :keyword ":inline>")))
      (is (= ":classes<"
             (printer/color-text :keyword ":classes<"))))
    (testing "unrecognized element html color markup"
      (is (= "in&lt;line"
             (printer/with-options inline-color
               (printer/color-text :bogus "in<line"))))
      (is (= "<span class=\"bogus\">&quot;classes</span>"
             (printer/with-options classes-color
               (printer/color-text :bogus "\"classes")))))
    (testing "happy path html color markup"
      (is (= (str "<span style=\"font-weight:bold;color:yellow\">"
                  ":in&amp;line</span>")
             (printer/with-options inline-color
               (printer/color-text :keyword ":in&line"))))
      (is (= "<span class=\"keyword\">:classes&lt;&gt;</span>"
             (printer/with-options classes-color
               (printer/color-text :keyword ":classes<>")))))
    (testing "escaping empty content"
      (is (= [:span] (html/escape-html-document ""))))))
