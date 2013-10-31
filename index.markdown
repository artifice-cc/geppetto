---
title: Geppetto
layout: default
---

<div style="width: 400px; float: right; text-align: center; padding: 10px;">
<img src="geppetto.jpg" align="right"/>
<a href="http://it.wikipedia.org/wiki/File:Le_avventure_di_Pinocchio-pag020.jpg">Image source</a>
</div>

<h1>Geppetto</h1>

<p class="lead">
A framework for experiments.
</p>

The Geppetto framework is a software library, a web-based user
interface, and an experimental methodology for designing, executing,
and analyzing software-based experiments.


## Benefits

- Any system that conforms to simple input/output formats can fully
  utilize Geppetto to initiate, collect, and analyze results from
  parameterized experiments.

- Configuration parameters, the source code version, the random seed
  (when appropriate), and other metadata are associated with
  experimental results, allowing experiments to be repeated with a
  single command.

- A web-based interface allows researchers to view tables of
  experimental results and generate plots and statistical analyses
  without writing any code.

- A specialized programming language allows system builders to
  document expected results, including expected outcomes of
  statistical tests, and to automatically verify that these
  expectations continue to be met as the system evolves.

- A large parameter space can be explored efficiently using simulated
  annealing combinatorial optimization algorithm; results are
  collected with other experimental results and can be analyzed in the
  same fashion.

## Download

Grab the code at [https://github.com/artifice-cc/geppetto](https://github.com/artifice-cc/geppetto)

## Screenshots

<div class="row">
  <div class="col-sm-6 col-md-4">
    <div class="thumbnail">
      <img src="images/screenshot-run-thumbnail.png" alt="Details about a run">
      <div class="caption">
        <h3>Details about a run</h3>
        <p>The results of an experiment are collected by Geppetto and an overview page is generated. Graphs and statistical analyses can be added. Other metadata, such as parameters, source code version, etc. are visible on the same page.</p>
        <p><a href="images/screenshot-run.png" class="btn btn-primary" role="button">View full screenshot</a></p>
      </div>
    </div>
  </div>
</div>

<h2>License <small>Geppetto is available under the MIT License.</small></h2>

<small>
<p>
Copyright (c) 2013 Joshua Eckroth
</p>
<p>
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:
</p>
<p>
The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.
</p>
<p>
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
</p>
</small>

