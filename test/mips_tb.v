`timescale 1us/1us
module mips_tb;

reg reset, clock;

// !!! Replace here
MIPS topLevel(.reset(reset), .clock(clock));

integer k;
initial begin
    reset = 1;
    clock = 0; #1;
    clock = 1; #1;
    clock = 0; #1;
    reset = 0; #1;
    
    // $stop;
	$dumpfile("test.vcd");
	$dumpvars;

    #1;
    for (k = 0; k < 5000; k = k + 1) begin
        clock = 1; #5;
        clock = 0; #5;
    end

    $finish;
end
    
endmodule
