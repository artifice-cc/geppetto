# Geppetto

[![Build Status](https://travis-ci.org/artifice-cc/geppetto.png)](https://travis-ci.org/artifice-cc/geppetto)

Experimental support for engineering intelligent systems.

## Usage examples

### Verify identical runs

```
lein run --action verify-identical --runid 123
```

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

Geppetto is available under the MIT License.

Copyright (c) 2013 Joshua Eckroth

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
