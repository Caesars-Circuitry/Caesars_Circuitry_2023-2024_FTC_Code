package org.firstinspires.ftc.teamcode.TeleOp;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;

@Config
@TeleOp
public class JackDrive extends OpMode {
    public DcMotor frontLeft;
    public DcMotor backLeft;
    public DcMotor frontRight;
    public DcMotor backRight;
    public DcMotor arm;
    public DcMotor intake;
    public Servo wall;

    public static double armDegrees = 0;
    public double ticks_in_degrees = 1425.1 / 360.0;

    public boolean isBoosted = false;
    public boolean ArmBoard = false;
    public boolean isSlowed = false;
    public String speedFactor = "None";

    public boolean RightControl = false;

    public boolean intakeActive = false;
    public String intakeDirection = "Normal";

    public double speedMultiplier = 0.75;

    public float lf_power = 0.0F;
    public float lb_power = 0.0F;
    public float rf_power = 0.0F;
    public float rb_power = 0.0F;

    @Override
    public void init() {
        frontLeft = hardwareMap.get(DcMotor.class, "M2");
        backLeft = hardwareMap.get(DcMotor.class, "M1");
        frontRight = hardwareMap.get(DcMotor.class, "M4");
        backRight = hardwareMap.get(DcMotor.class, "M3");
        arm = hardwareMap.get(DcMotor.class, "arm");
        intake = hardwareMap.get(DcMotor.class, "intakeMotor");
        wall = hardwareMap.get(Servo.class, "intakeServo");

        frontLeft.setDirection(DcMotor.Direction.FORWARD);
        backLeft.setDirection(DcMotor.Direction.FORWARD);
        frontRight.setDirection(DcMotor.Direction.REVERSE);
        backRight.setDirection(DcMotor.Direction.REVERSE);
        arm.setDirection(DcMotor.Direction.FORWARD);

        frontLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        backLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        frontRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        backRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        arm.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        arm.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        telemetry.addData("Status", "Initialized");
        telemetry.update();

        wall.setPosition(1);

        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
    }

    public void loop() {
        telemetry.addData("Status", "Running");

        float x = -gamepad1.right_stick_x;
        float y = gamepad1.left_stick_y;
        float turn = -gamepad1.left_stick_x / 2;

        FindJoystickMovement(x, y, turn);

        try {
            FindMovementMultiplier();
            ActivateIntake();
            RightControlActivation();
            MoveArm();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        float leftFrontPower = lf_power;
        float leftBackPower = lb_power;
        float rightFrontPower = rf_power;
        float rightBackPower = rb_power;


        frontLeft.setPower(leftFrontPower * speedMultiplier);
        backLeft.setPower(leftBackPower * speedMultiplier);
        frontRight.setPower(rightFrontPower * speedMultiplier);
        backRight.setPower(rightBackPower * speedMultiplier);



        AddTelemetry();
        telemetry.update();
    }

    public void FindJoystickMovement(float x, float y, float turn) {
        float theta = (float) Math.atan2(y, x);
        float power = (float) Math.hypot(x, y);

        float sin = (float) Math.sin(theta - Math.PI / 4);
        float cos = (float) Math.cos(theta - Math.PI / 4);
        float max = Math.max(Math.abs(sin), Math.abs(cos));

        lf_power = power * cos / max + turn;
        lb_power = power * sin / max + turn;
        rf_power = power * sin / max - turn;
        rb_power = power * cos / max - turn;

        if ((power + Math.abs(turn)) > 1) {
            lf_power /= power + turn;
            lb_power /= power + turn;
            rf_power /= power + turn;
            rb_power /= power + turn;
        }

    }

    public void FindMovementMultiplier() throws InterruptedException {
        if (gamepad1.left_trigger>0 && !isBoosted && !isSlowed) {
            isBoosted = true;
            speedMultiplier = 2.5;
            speedFactor = "Boosted";
            Thread.sleep(500);
        }
        else if (gamepad1.dpad_up && !isSlowed) {
            isSlowed = true;
            speedMultiplier = .25;
            speedFactor = "Slow";
            Thread.sleep(500);
        }
        else if ((gamepad1.right_trigger>0 && isSlowed && !isBoosted)){
            isSlowed = false;
            isBoosted = false;
            speedMultiplier = 0.75;
            speedFactor = "Normal";
            Thread.sleep(500);
        }

    }

    public void MoveArm() throws InterruptedException {
        if (RightControl) {
            if (gamepad2.right_stick_y < 0) {
                armDegrees += .5;
            }
            else if (gamepad2.right_stick_y > 0) {
                armDegrees -= .5;
            }
        }
        else {
            if (gamepad1.a) {
                arm.setPower(.5);
                armDegrees = 138;
                wait(500);
                armDegrees = 46;
                arm.setPower(0);
            }
        }

        arm.setPower(.5);
        arm.setTargetPosition((int) (ticks_in_degrees * armDegrees));
        arm.setMode(DcMotor.RunMode.RUN_TO_POSITION);

    }

    public void ActivateIntake( )throws InterruptedException {
        if(gamepad1.left_bumper & !intakeActive) {
            wall.setPosition(.5);
            intake.setPower(.2);
            intakeDirection = "Forward";
            intakeActive = true;
            Thread.sleep(500);
        }
        else if(gamepad1.right_bumper & intakeActive) {
            wall.setPosition(0);
            intake.setPower(0);
            intakeDirection = "None";
            intakeActive = false;
            Thread.sleep(500);
        }
    }

    public void RightControlActivation() throws InterruptedException {
        if(gamepad2.right_bumper && !RightControl) {
            RightControl = true;
            Thread.sleep(500);
        }

        else if(gamepad2.right_bumper && RightControl) {
            RightControl = false;
            Thread.sleep(500);
        }
    }

    public void AddTelemetry() {
        telemetry.addLine("\u001B[1m" + "Intake");
        telemetry.addData("Intake Direction: ", intakeDirection);
        telemetry.addData("Intake Active: ", intakeActive);

        telemetry.addLine("\n");

        telemetry.addLine("\u001B[1m" + "Arm");
        telemetry.addData("Arm Angle: ", armDegrees);

        telemetry.addLine("\n");

        telemetry.addLine("\u001B[1m" + "Right Control");
        telemetry.addData("Right Control: ", RightControl);

        telemetry.addLine("\n");

        telemetry.addLine("\u001B[1m" + "Wheel Speed Factor");
        telemetry.addData("Speed Factor: ", speedFactor);

        telemetry.addLine("\n");

        telemetry.addLine("\u001B[1m" + "Wheel Speeds");
        telemetry.addData("Left Front Power: ", lf_power * speedMultiplier);
        telemetry.addData("Left Back Power: ", lb_power * speedMultiplier);
        telemetry.addData("Right Front Power: ", rf_power * speedMultiplier);
        telemetry.addData("Right Back Power: ", rb_power * speedMultiplier);
    }
}