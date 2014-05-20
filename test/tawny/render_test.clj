;; The contents of this file are subject to the LGPL License, Version 3.0.
;;
;; Copyright (C) 2013, 2014, Phillip Lord, Newcastle University
;;
;; This program is free software: you can redistribute it and/or modify it
;; under the terms of the GNU Lesser General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or (at your
;; option) any later version.
;;
;; This program is distributed in the hope that it will be useful, but WITHOUT
;; ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
;; FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
;; for more details.
;;
;; You should have received a copy of the GNU Lesser General Public License
;; along with this program. If not, see http://www.gnu.org/licenses/.
(ns tawny.render-test
  (:use [clojure.test])
  (:require [tawny.render :as r]
            [tawny.owl :as o]
            [tawny.fixture]
            ))

(def to nil)

(defn createtestontology[test]
  (alter-var-root
   #'to
   (fn [x]
     (o/ontology :iri "http://iri/"
                 :noname true
                 :prefix "iri")))
  (test))

(use-fixtures :each createtestontology)

(deftest datatype
  (is (= :XSD_INTEGER
         (r/as-form (#'o/ensure-datatype to :XSD_INTEGER)))))

(defn lit-f
  ([val]
     (r/as-form (o/literal val)))
  ([val lang]
     (r/as-form (o/literal val :lang lang))))

(deftest literal
  (is
   (= '(literal "10" :type :XSD_INTEGER)
      (lit-f 10))))
;;   (is
;;    (= [10.0]
;;       (lit-f 10.0)))
;;   (is
;;    (= [true]
;;       (lit-f true)))
;;   (is
;;    (= ["bob"]
;;       (lit-f "bob")))
;;   (is
;;    (= ["bob" "en"]
;;       (lit-f "bob" "en"))))

(defn data-ontology []
  (o/datatype-property to "rD"))

(deftest datasome-datatype
  (is
   (=
    '(owl-some (iri "http://iri/#rD") :XSD_INTEGER)
    (do (data-ontology)
        (r/as-form
         (o/owl-some to "rD" :XSD_INTEGER))))))

(deftest datasome-range
  (is
   (= '(owl-some (iri "http://iri/#rD") (span < 1))
      (r/as-form
       (o/owl-some to "rD" (o/span < 1))))))


(deftest individual-fact-1
  (is
   (= '(individual (iri "http://iri/#I")
                   :fact
                   (fact (iri "http://iri/#r")
                         (iri "http://iri/#I2")))
      (r/as-form
       (o/individual to "I"
                     :fact (o/fact to (o/object-property to "r")
                                   (o/individual to "I2")))))))

(deftest individual-fact-2
  (is
   (= '(individual (iri "http://iri/#I")
                   :fact
                   (fact-not (iri "http://iri/#r")
                             (iri "http://iri/#I2")))
      (r/as-form
       (o/individual to "I"
                     :fact (o/fact-not to (o/object-property to "r")
                                   (o/individual to "I2")))))))


(deftest individual-3
  (is
   (=
    `(individual (iri "http://iri/#I")
                :fact
                (fact (iri "http://iri/#r")
                         (iri "http://iri/#I2"))
                (fact-not (iri "http://iri/#r")
                          (iri "http://iri/#I2"))))
   (r/as-form
    (o/individual to "I"
                  :fact
                     (o/fact to (o/object-property to "r")
                             (o/individual to "I2"))
                     (o/fact-not to (o/object-property to "r")
                                 (o/individual to "I2"))))))


(deftest individual-data
  (is
   (=
    '(individual
      (iri "http://iri/#I")
      :fact (fact (iri "http://iri/#d")
                  (literal "10" :type :XSD_INTEGER)))
    (r/as-form
     (o/individual to "I"
                   :fact
                   (o/fact to (o/datatype-property to "d")
                           10))))))


(deftest individual-data-2
  (is
   (= '(individual (iri "http://iri/#I")
                   :fact
                   (fact (iri "http://iri/#r")
                         (iri "http://iri/#I2"))
                   (fact (iri "http://iri/#d")
                         (literal "10" :type :XSD_INTEGER)))
      (r/as-form
       (o/individual to "I"
                     :fact
                     (o/fact to (o/datatype-property to "d")
                             10)
                     (o/fact to (o/object-property to "r")
                             (o/individual to "I2")))))))

(deftest oproperty-super-test
  (is
   (= '(object-property
        (iri "http://iri/#r")
        :super
        (iri "http://iri/#s"))
      (r/as-form
       (o/object-property to "r"
                          :super
                          (o/iri-for-name to "s"))))))

(deftest dproperty-super-test
  (is
   (= '(datatype-property
        (iri "http://iri/#g")
        :super
        (iri "http://iri/#h"))
      (r/as-form
       (o/datatype-property to "g"
                          :super
                          (o/iri-for-name to "h"))))))


(deftest entity-or-iri-object
  (is
   (tawny.owl/with-probe-entities to
     [a (o/owl-class to "a")]
     (=
      (r/as-form
       (o/owl-and to a) :object true)
      ['owl-and a]))))


(defn multi-as-form [entity]
  (vector
   (r/as-form entity :object true)
   (r/as-form entity :object true :explicit true)
   (r/as-form entity :object true :keyword true)
   (r/as-form entity :object true :explicit true :keyword true)
   ))

;; :some
(deftest object-some
  (is
   (o/with-probe-entities to
     [p (o/object-property to "p")
      c (o/owl-class to "c")]
     (= [
         ['owl-some p c]
         ['object-some p c]
         [:some p c]
         [:object-some p c]]
        (multi-as-form
          (o/owl-some p c))))))


(deftest data-some
  (is
   (o/with-probe-entities to
     [d (o/datatype-property to "d")]
     (=
      [
       ['owl-some d :XSD_INTEGER]
       ['data-some d :XSD_INTEGER]
       [:some d :XSD_INTEGER]
       [:data-some d :XSD_INTEGER]]
      (multi-as-form
       (o/owl-some d :XSD_INTEGER))))

     ))

;; :only

(deftest object-only
  (is
   (tawny.owl/with-probe-entities to
     [p (o/object-property to "p")
      c (o/owl-class to "c")]
     (=
      [
       ['only p c]
       ['object-only p c]
       [:only p c]
       [:object-only p c]]
      (multi-as-form
       (o/only p c))))))

(deftest data-only
  (is
   (o/with-probe-entities to
     [d (o/datatype-property to "d")]
     (=
      [
       ['only d :XSD_INTEGER]
       ['data-only d :XSD_INTEGER]
       [:only d :XSD_INTEGER]
       [:data-only d :XSD_INTEGER]]
      (multi-as-form
       (o/only d :XSD_INTEGER))))))


;; :owl-and

(deftest object-and
  (is
   (o/with-probe-entities to
     [b (o/owl-class to "b")
      c (o/owl-class to "c")]
     (= [
         ['owl-and b c]
         ['object-and b c]
         [:and b c]
         [:object-and b c]]
        (multi-as-form
          (o/owl-and b c))))))


(deftest data-and
  (is
   (=
    [
     ['owl-and :XSD_INTEGER]
     ['data-and :XSD_INTEGER]
     [:and  :XSD_INTEGER]
     [:data-and  :XSD_INTEGER]]
    (multi-as-form
     (o/owl-and :XSD_INTEGER)))))


;; :owl-or
(deftest object-or
  (is
   (o/with-probe-entities to
     [b (o/owl-class to "b")
      c (o/owl-class to "c")]
     (= [
         ['owl-or b c]
         ['object-or b c]
         [:or b c]
         [:object-or b c]]
        (multi-as-form
          (o/owl-or b c))))))


(deftest data-or
  (is
   (=
     [
      ['owl-or :XSD_INTEGER]
      ['data-or :XSD_INTEGER]
      [:or :XSD_INTEGER]
      [:data-or :XSD_INTEGER]]
     (multi-as-form
      (o/owl-or :XSD_INTEGER)))))




;; :exactly
(deftest object-exactly
  (is
   (tawny.owl/with-probe-entities to
     [p (o/object-property to "p")
      c (o/owl-class to "c")]
     (=
      [
       ['exactly 1 p c]
       ['object-exactly 1 p c]
       [:exactly 1 p c]
       [:object-exactly 1 p c]]
      (multi-as-form
       (o/exactly 1 p c))))))

(deftest data-exactly
  (is
   (tawny.owl/with-probe-entities to
     [d (o/datatype-property to "d")]
     (=
      [
       ['exactly 1 d :RDFS_LITERAL]
       ['data-exactly 1 d :RDFS_LITERAL]
       [:exactly 1 d :RDFS_LITERAL]
       [:data-exactly 1 d :RDFS_LITERAL]]
      (multi-as-form
       (o/exactly 1 d))))))




;; :oneof
(deftest object-oneof
  (is
   (tawny.owl/with-probe-entities to
     [i (o/individual "i")]
     (=
      [
       ['oneof i]
       ['object-oneof i]
       [:oneof i]
       [:object-oneof i]]
      (multi-as-form
       (o/oneof i))))))

(deftest data-oneof
  (is
   (=
    [
     ['oneof ['literal "10" :type :XSD_INTEGER]]
     ['data-oneof ['literal "10" :type :XSD_INTEGER]]
     [:oneof [:literal "10" :type :XSD_INTEGER]]
     [:data-oneof [:literal "10" :type :XSD_INTEGER]]]
    (multi-as-form
     (o/oneof (o/literal 10))))))

;; :at-least
(deftest object-at-least
  (is
   (tawny.owl/with-probe-entities to
     [p (o/object-property to "p")
      c (o/owl-class to "c")]
     (=
      [
       ['at-least 1 p c]
       ['object-at-least 1 p c]
       [:at-least 1 p c]
       [:object-at-least 1 p c]]
      (multi-as-form
       (o/at-least 1 p c))))))

(deftest data-at-least
  (is
   (tawny.owl/with-probe-entities to
     [d (o/datatype-property to "d")]
     (=
      [
       ['at-least 1 d :RDFS_LITERAL]
       ['data-at-least 1 d :RDFS_LITERAL]
       [:at-least 1 d :RDFS_LITERAL]
       [:data-at-least 1 d :RDFS_LITERAL]]
      (multi-as-form
       (o/at-least 1 d))))))

;; :at-most
(deftest object-at-most
  (is
   (tawny.owl/with-probe-entities to
     [p (o/object-property to "p")
      c (o/owl-class to "c")]
     (=
      [
       ['at-most 1 p c]
       ['object-at-most 1 p c]
       [:at-most 1 p c]
       [:object-at-most 1 p c]]
      (multi-as-form
       (o/at-most 1 p c))))))

(deftest data-at-most
  (is
   (tawny.owl/with-probe-entities to
     [d (o/datatype-property to "d")]
     (=
      [
       ['at-most 1 d :RDFS_LITERAL]
       ['data-at-most 1 d :RDFS_LITERAL]
       [:at-most 1 d :RDFS_LITERAL]
       [:data-at-most 1 d :RDFS_LITERAL]]
      (multi-as-form
       (o/at-most 1 d))))))

;; :has-value
(deftest object-has-value
  (is
   (tawny.owl/with-probe-entities to
     [r (o/object-property "r")
      i (o/individual "i")]
     (=
      [['has-value r i]
       ['object-has-value r i]
       [:has-value r i]
       [:object-has-value r i]]
      (multi-as-form
       (o/has-value r i))))))

(deftest data-has-value
  (is
   (tawny.owl/with-probe-entities to
     [d (o/datatype-property "d")]
     (=
      [['has-value d ['literal "10" :type :XSD_INTEGER]]
       ['data-has-value d ['literal "10" :type :XSD_INTEGER]]
       [:has-value d [:literal "10" :type :XSD_INTEGER]]
       [:data-has-value d [:literal "10" :type :XSD_INTEGER]]]
      (multi-as-form
       (o/has-value d 10))))))

;; :owl-not
(deftest object-owl-not
  (is
   (tawny.owl/with-probe-entities to
     [c (o/owl-class to "c")]
     (=
      [['owl-not c]
       ['object-not c]
       [:not c]
       [:object-not c]]
      (multi-as-form
       (o/owl-not to c))))))

(deftest data-owl-not
  (is
   (=
    [['owl-not :XSD_INTEGER]
     ['data-not :XSD_INTEGER]
     [:not :XSD_INTEGER]
     [:data-not :XSD_INTEGER]]
    (multi-as-form
     (o/owl-not :XSD_INTEGER)))))


;; :iri
(deftest iri
  (is
   (=
    [['iri "i"]
     ['iri "i"]
     [:iri "i"]
     [:iri "i"]]
    (multi-as-form
     (o/iri "i")))))

;; :label
(deftest label
  (is
   (=
    [['label ['literal "l" :lang "en"]]
     ['label ['literal "l" :lang "en"]]
     [:label [:literal "l" :lang "en"]]
     [:label [:literal "l" :lang "en"]]])
   (multi-as-form
    (o/label "l"))))

;; :comment
(deftest label
  (is
   (=
    [['owl-comment ['literal "l" :lang "en"]]
     ['owl-comment ['literal "l" :lang "en"]]
     [:comment [:literal "l" :lang "en"]]
     [:comment [:literal "l" :lang "en"]]])
   (multi-as-form
    (o/owl-comment "l"))))

;; :literal
(deftest literal
  (is
   (=
    [['literal "1" :type :XSD_INTEGER]
     ['literal "1" :type :XSD_INTEGER]
     [:literal "1" :type :XSD_INTEGER]
     [:literal "1" :type :XSD_INTEGER]]
    (multi-as-form
     (o/literal 1)))))

;; :<
(deftest span<
  (is
   (=
    [['span '< 0]
     ['span '< 0]
     [:span :< 0]
     [:span :< 0]]
    (multi-as-form
     (o/span < 0)))))

;; :<=
(deftest span<=
  (is
   (=
    [['span '<= 0]
     ['span '<= 0]
     [:span :<= 0]
     [:span :<= 0]]
    (multi-as-form
     (o/span <= 0)))))


;; :>
(deftest span>
  (is
   (=
    [['span '> 0]
     ['span '> 0]
     [:span :> 0]
     [:span :> 0]]
    (multi-as-form
     (o/span < 0)))))

;; :>=
(deftest span>
  (is
   (=
    [['span '>= 0]
     ['span '>= 0]
     [:span :>= 0]
     [:span :>= 0]]
    (multi-as-form
     (o/span >= 0)))))

;; :has-self
(deftest hasself
  (is
   (tawny.owl/with-probe-entities to
     [r (o/object-property "r")]
     (=
      [['has-self r]
       ['has-self r]
       [:has-self r]
       [:has-self r]]
      (multi-as-form
       (o/has-self r))))))

;; :inverse
(deftest inverse
  (is
   (tawny.owl/with-probe-entities to
     [r (o/object-property "r")]
     (=
      [['inverse r]
       ['object-inverse r]
       [:inverse r]
       [:object-inverse r]]))))


(defn double-as-form [entity]
  (vector
   (r/as-form entity :object true)
   (r/as-form entity :object true :keyword true)))

(deftest owl-class
  (is
   (tawny.owl/with-probe-entities to
     [c (o/owl-class to "c")
      d (o/owl-class to "d")
      e (o/owl-class to "e"
                     :super c d)]
     (=
      [
       ['owl-class e :super c d]
       [:class e :super [c d]]]
      (double-as-form e)))))


(deftest oproperty
  (is
   (tawny.owl/with-probe-entities to
     [r (o/object-property "r")]
     (=
      [['object-property r]
       [:oproperty r]])
     (double-as-form r))))

;; :annotation


