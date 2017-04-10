class Link : Object [
   | _value link |
   class withValue: v [
       "Call Object>>new, then set _value to arg v. same as '^self new' here
        since we don't define new in Link."
       ^super new value: v.
   ]

   nextLink [ ^link ]
   nextLink: aLink [ link := aLink ]
   value: v [ _value := v. ]
   value [^_value]
   asString [^_value asString]
]

