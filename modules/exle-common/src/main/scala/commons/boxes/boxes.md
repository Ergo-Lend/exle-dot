# Boxes Pattern
This document consists of a few design patterns for utility lendcore.commons.boxes in the form of Ergo's EUTXO Boxes.

## ServiceBox
It acts as a form of accounting that supervises the tokenized lendcore.commons.boxes that it creates. These lendcore.commons.boxes interacts through creation of tokenized lendcore.commons.boxes and consumption of them too.

Other than that, this box distributes the funds to the owner whenever a tokenized box is consumed.


