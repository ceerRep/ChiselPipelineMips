module InstMemoryBlackBox (
    input [31:0] addr,
    output [31:0] dout
);
    reg [31:0] mem[2047:0];
    wire [10:0] index;

    assign index = addr[12:2];
    initial $readmemh("/tmp/code.txt", mem);

    assign dout = mem[index];
endmodule