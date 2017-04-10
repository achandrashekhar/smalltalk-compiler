class T [
 do: blk [
  |p|
  p:=head.
  [p~~nil] whileTrue: [ blk value: (p value). p := p nextLink ]
 ]
]
