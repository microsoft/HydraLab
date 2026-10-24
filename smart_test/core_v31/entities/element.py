from typing import Optional

from common.constants import android_widget


class Element:
    def __init__(self, element_type, element_class, text, text_hint, content_desc, resource_id, checkable, checked,
                 clickable, enabled, focusable, long_clickable, password, scrollable, selected, bounds, displayed,
                 xpath, ancestors, image_override):
        self.element_type = element_type
        self.element_class = element_class
        self.text = text
        self.text_hint = text_hint
        self.content_desc = content_desc
        self.resource_id = resource_id
        self.checkable = checkable
        self.checked = checked
        self.clickable = clickable
        self.enabled = enabled
        self.focusable = focusable
        self.long_clickable = long_clickable
        self.password = password
        self.scrollable = scrollable
        self.selected = selected
        self.bounds = [int(i) for i in bounds[1: len(bounds) - 1].replace("][", ",").split(",")]
        self.displayed = displayed
        self.xpath = xpath
        self.ancestors = ancestors
        self.image_override = image_override

    def __str__(self):
        return f"[{self.element_type.capitalize()}] Class: {self.element_class}, Text: {self.text}, " \
               f"Description: {self.content_desc[0]}, ResourceID: {self.resource_id}, " \
               f"ElementType: {self.element_type}, Clickable: {self.clickable}, " \
               f"LongClickable: {self.long_clickable}, scrollable: {self.scrollable}, Bounds: {self.bounds}, "

    def __eq__(self, o):
        # todo: may need to comment bounds as some elements are scalable hence bounds may change
        return self.element_class == o.element_class \
               and self.bounds == o.bounds  \
               and self.content_desc == o.content_desc \
               and self.text_hint == o.text_hint \
               and self.resource_id == o.resource_id \
               and (self.text == o.text or self.element_class in android_widget.EDIT_TEXT or self.element_class in android_widget.RECYCLER_VIEW) # EditText text would change

    def __hash__(self):
        return hash((self.element_class, self.content_desc[0], self.text, self.text_hint))

    def interactive(self):
        return self.element_type == 'input' or self.element_type == 'clickable'

    def prompt_format(self, id):
        bounds_size = len(self.bounds)
        if bounds_size < 4 or self.bounds[2] <= self.bounds[0] or self.bounds[3] <= self.bounds[1]:
            element_size = 0
        else:
            element_size = (self.bounds[2] - self.bounds[0]) * (self.bounds[3] - self.bounds[1])

        # return f"<id=\"{id}\" element_type=\"{self.element_type}\" element_class=\"{self.element_class}\" " \
        #        f"text=\"{self.text}\" description=\"{self.content_desc[0]}\" resource_id=\"{self.resource_id}\" " \
        #        f"clickable=\"{self.clickable}\" long_clickable=\"{self.long_clickable}\" " \
        #        f"scrollable=\"{self.scrollable}\" element_size=\"{element_size}\">"

        return f"<id=\"{id}\" element_type=\"{self.element_type}\" element_class=\"{self.element_class}\" " \
               f"text=\"{self.text}\" description=\"{self.content_desc[0]}\" resource_id=\"{self.resource_id}\" " \
               f"clickable=\"{self.clickable}\" long_clickable=\"{self.long_clickable}\" " \
               f"scrollable=\"{self.scrollable}\" bounds=\"{self.bounds}\" element_size=\"{element_size}\">"
