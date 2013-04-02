# geppetto

A Clojure library designed to ... well, that part is up to you.

## Usage

FIXME

## Examples

### Claims

```clojure
(make-claim tracking-baseline-high-avgprec
  (parameters "Tracking/baseline")
  (verify (> (mean :AvgPrec) 0.75)
          (> (mean :AvgCoverage) 0.75)))

(make-claim tracking-noise-lowers-avgprec
  (parameters "Tracking/noise")
  (depends [tracking-baseline-high-avgprec])
  (verify (linear-reg :SensorInsertionNoise :AvgPrec
                      (< slope 0.0) (> adj-r2 0.3))))

(make-claim tracking-meta
  (parameters "Tracking/meta")
  (depends [tracking-noise-lowers-avgprec])
  (verify
   (each-of :Metareasoning
            (paired-t-test :AvgPrec (< p-value 0.01)
                           (> mean-diff 0.1)))))
```

## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
