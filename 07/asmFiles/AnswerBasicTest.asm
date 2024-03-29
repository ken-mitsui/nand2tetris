// push constant 10
@10
D=A
@SP
A=M
M=D
@SP
M=M+1

// pop local 0
@LCL
D=M // D <- 300(=R[1])
@0 // arg2
D=D+A // D <- 300
@R13
M=D // R[13] <- 300
@SP
AM=M-1 // A,R[0] <- 256
D=M // D <- 10(=R[256])
@R13
A=M // A <- 300
M=D // R[300] <- 10

// push constant 21
@21
D=A
@SP
A=M
M=D
@SP
M=M+1

// push constant 22
@22
D=A
@SP
A=M
M=D
@SP
M=M+1

// pop argument 2
@ARG
D=M
@2
D=D+A
@R13
M=D
@SP
AM=M-1
D=M
@R13
A=M
M=D

// pop argument 1
@ARG
D=M
@1
D=D+A
@R13
M=D
@SP
AM=M-1
D=M
@R13
A=M
M=D

// push constant 36
@36
D=A
@SP
A=M
M=D
@SP
M=M+1

// pop this 6
@THIS
D=M
@6
D=D+A
@R13
M=D
@SP
AM=M-1
D=M
@R13
A=M
M=D

// push constant 42
@42
D=A
@SP
A=M
M=D
@SP
M=M+1

// push constant 45
@45
D=A
@SP
A=M
M=D
@SP
M=M+1

// pop that 5
@THAT
D=M
@5
D=D+A
@R13
M=D
@SP
AM=M-1
D=M
@R13
A=M
M=D

// pop that 2
@THAT
D=M
@2
D=D+A
@R13
M=D
@SP
AM=M-1
D=M
@R13
A=M
M=D

// push constant 510
@510
D=A
@SP
A=M
M=D
@SP
M=M+1

// pop temp 6
@R5
D=A
@6
D=D+A
@R13
M=D
@SP
AM=M-1
D=M
@R13
A=M
M=D

// push local 0
@LCL
D=M
@0
D=D+A
A=D
D=M
@SP
A=M
M=D
@SP
M=M+1

// push that 5
@THAT
D=M
@5
D=D+A
A=D
D=M
@SP
A=M
M=D
@SP
M=M+1

// add
@SP
AM=M-1
D=M
A=A-1
D=M+D // add
@SP
A=M-1
M=D

// push argument 1
@ARG
D=M
@1
D=D+A
A=D
D=M
@SP
A=M
M=D
@SP
M=M+1

// sub
@SP
AM=M-1
D=M
A=A-1
D=M-D // sub
@SP
A=M-1
M=D

// push this 6
@THIS
D=M
@6
D=D+A
A=D
D=M
@SP
A=M
M=D
@SP
M=M+1

// push this 6
@THIS
D=M
@6
D=D+A
A=D
D=M
@SP
A=M
M=D
@SP
M=M+1

// add
@SP
AM=M-1
D=M
A=A-1
D=M+D // add
@SP
A=M-1
M=D

// sub
@SP
AM=M-1
D=M
A=A-1
D=M-D // sub
@SP
A=M-1
M=D

// push temp 6
@R5
D=A
@6
D=D+A
A=D
D=M
@SP
A=M
M=D
@SP
M=M+1

// add
@SP
AM=M-1
D=M
A=A-1
D=M+D // add
@SP
A=M-1
M=D
