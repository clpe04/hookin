(ns hookin.core-test
  (:use clojure.test
        hookin.core))

(defn test-function
  [x]
  (+ x x))

(deftest hookfn-test
  (let [hook-fn (hookfn test-fn :test [x] (* x 2))]
    (testing "Creating a hooked function through hookin"
      (is (fn? test-fn)))
    (testing "Checking for defined hookin keyword in created function"
      (is (not (nil? (*hook-keyword* (meta hook-fn))))))))