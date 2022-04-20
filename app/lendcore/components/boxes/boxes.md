# Boxes Pattern
This document consists of a few design patterns for utility lendcore.components.boxes in the form of Ergo's EUTXO Boxes.

## ServiceBox
It acts as a form of accounting that supervises the tokenized lendcore.components.boxes that it creates. These lendcore.components.boxes interacts through creation of tokenized lendcore.components.boxes and consumption of them too.

Other than that, this box distributes the funds to the owner whenever a tokenized box is consumed.


