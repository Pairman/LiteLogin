package org.eu.pnxlr.git.litelogin.core.command;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.BuiltInExceptionProvider;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import org.eu.pnxlr.git.litelogin.api.internal.util.Pair;

/**
 * Custom messages.
 */
public class BuiltInExceptions implements BuiltInExceptionProvider {
    private final Dynamic2CommandExceptionType doubleTooSmall;
    private final Dynamic2CommandExceptionType doubleTooBig;
    private final Dynamic2CommandExceptionType floatTooSmall;
    private final Dynamic2CommandExceptionType floatTooBig;
    private final Dynamic2CommandExceptionType integerTooSmall;
    private final Dynamic2CommandExceptionType integerTooBig;
    private final Dynamic2CommandExceptionType longTooSmall;
    private final Dynamic2CommandExceptionType longTooBig;
    private final DynamicCommandExceptionType literalIncorrect;
    private final SimpleCommandExceptionType readerExpectedStartOfQuote;
    private final SimpleCommandExceptionType readerExpectedEndOfQuote;
    private final DynamicCommandExceptionType readerInvalidEscape;
    private final DynamicCommandExceptionType readerInvalidBool;
    private final DynamicCommandExceptionType readerInvalidInt;
    private final SimpleCommandExceptionType readerExpectedInt;
    private final DynamicCommandExceptionType readerInvalidLong;
    private final SimpleCommandExceptionType readerExpectedLong;
    private final DynamicCommandExceptionType readerInvalidDouble;
    private final SimpleCommandExceptionType readerExpectedDouble;
    private final DynamicCommandExceptionType readerInvalidFloat;
    private final SimpleCommandExceptionType readerExpectedFloat;
    private final SimpleCommandExceptionType readerExpectedBool;
    private final DynamicCommandExceptionType readerExpectedSymbol;
    private final SimpleCommandExceptionType dispatcherUnknownCommand;
    private final SimpleCommandExceptionType dispatcherUnknownArgument;
    private final SimpleCommandExceptionType dispatcherExpectedArgumentSeparator;
    private final DynamicCommandExceptionType dispatcherParseException;

    public BuiltInExceptions() {
        doubleTooSmall = new Dynamic2CommandExceptionType((found, min) -> new LiteralMessage(msg("The number must not be smaller than {min}. Found {found}.", new Pair<>("found", found), new Pair<>("min", min))));
        doubleTooBig = new Dynamic2CommandExceptionType((found, max) -> new LiteralMessage(msg("The number must not be larger than {max}. Found {found}.", new Pair<>("found", found), new Pair<>("max", max))));
        floatTooSmall = new Dynamic2CommandExceptionType((found, min) -> new LiteralMessage(msg("The number must not be smaller than {min}. Found {found}.", new Pair<>("found", found), new Pair<>("min", min))));
        floatTooBig = new Dynamic2CommandExceptionType((found, max) -> new LiteralMessage(msg("The number must not be larger than {max}. Found {found}.", new Pair<>("found", found), new Pair<>("max", max))));
        integerTooSmall = new Dynamic2CommandExceptionType((found, min) -> new LiteralMessage(msg("The integer must not be smaller than {min}. Found {found}.", new Pair<>("found", found), new Pair<>("min", min))));
        integerTooBig = new Dynamic2CommandExceptionType((found, max) -> new LiteralMessage(msg("The integer must not be larger than {max}. Found {found}.", new Pair<>("found", found), new Pair<>("max", max))));
        longTooSmall = new Dynamic2CommandExceptionType((found, min) -> new LiteralMessage(msg("The long value must not be smaller than {min}. Found {found}.", new Pair<>("found", found), new Pair<>("min", min))));
        longTooBig = new Dynamic2CommandExceptionType((found, max) -> new LiteralMessage(msg("The long value must not be larger than {max}. Found {found}.", new Pair<>("found", found), new Pair<>("max", max))));
        literalIncorrect = new DynamicCommandExceptionType(expected -> new LiteralMessage(msg("Expected literal {expected}.", new Pair<>("expected", expected))));
        readerExpectedStartOfQuote = new SimpleCommandExceptionType(new LiteralMessage("Expected a quote to start the string."));
        readerExpectedEndOfQuote = new SimpleCommandExceptionType(new LiteralMessage("Unclosed quoted string."));
        readerInvalidEscape = new DynamicCommandExceptionType(character -> new LiteralMessage(msg("Invalid escape sequence {character} in quoted string.", new Pair<>("character", character))));
        readerInvalidBool = new DynamicCommandExceptionType(value -> new LiteralMessage(msg("Invalid boolean value {value}. Expected true or false.", new Pair<>("value", value))));
        readerInvalidInt = new DynamicCommandExceptionType(value -> new LiteralMessage(msg("Invalid integer {value}.", new Pair<>("value", value))));
        readerExpectedInt = new SimpleCommandExceptionType(new LiteralMessage("Expected an integer."));
        readerInvalidLong = new DynamicCommandExceptionType(value -> new LiteralMessage(msg("Invalid long value {value}.", new Pair<>("value", value))));
        readerExpectedLong = new SimpleCommandExceptionType(new LiteralMessage("Expected a long value."));
        readerInvalidDouble = new DynamicCommandExceptionType(value -> new LiteralMessage(msg("Invalid double value {value}.", new Pair<>("value", value))));
        readerExpectedDouble = new SimpleCommandExceptionType(new LiteralMessage("Expected a double value."));
        readerInvalidFloat = new DynamicCommandExceptionType(value -> new LiteralMessage(msg("Invalid float value {value}.", new Pair<>("value", value))));
        readerExpectedFloat = new SimpleCommandExceptionType(new LiteralMessage("Expected a float value."));
        readerExpectedBool = new SimpleCommandExceptionType(new LiteralMessage("Expected a boolean value."));
        readerExpectedSymbol = new DynamicCommandExceptionType(symbol -> new LiteralMessage(msg("Expected {symbol}.", new Pair<>("symbol", symbol))));
        dispatcherUnknownCommand = new SimpleCommandExceptionType(new LiteralMessage("Unknown command."));
        dispatcherUnknownArgument = new SimpleCommandExceptionType(new LiteralMessage("Incorrect command arguments."));
        dispatcherExpectedArgumentSeparator = new SimpleCommandExceptionType(new LiteralMessage("Expected whitespace to end one argument, but found trailing data."));
        dispatcherParseException = new DynamicCommandExceptionType(message -> new LiteralMessage(msg("Could not parse command {command}.", new Pair<>("command", message))));
    }

    private static String msg(String template, Pair<?, ?>... pairs) {
        return org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(template, pairs);
    }

    @Override
    public Dynamic2CommandExceptionType doubleTooLow() {
        return doubleTooSmall;
    }

    @Override
    public Dynamic2CommandExceptionType doubleTooHigh() {
        return doubleTooBig;
    }

    @Override
    public Dynamic2CommandExceptionType floatTooLow() {
        return floatTooSmall;
    }

    @Override
    public Dynamic2CommandExceptionType floatTooHigh() {
        return floatTooBig;
    }

    @Override
    public Dynamic2CommandExceptionType integerTooLow() {
        return integerTooSmall;
    }

    @Override
    public Dynamic2CommandExceptionType integerTooHigh() {
        return integerTooBig;
    }

    @Override
    public Dynamic2CommandExceptionType longTooLow() {
        return longTooSmall;
    }

    @Override
    public Dynamic2CommandExceptionType longTooHigh() {
        return longTooBig;
    }

    @Override
    public DynamicCommandExceptionType literalIncorrect() {
        return literalIncorrect;
    }

    @Override
    public SimpleCommandExceptionType readerExpectedStartOfQuote() {
        return readerExpectedStartOfQuote;
    }

    @Override
    public SimpleCommandExceptionType readerExpectedEndOfQuote() {
        return readerExpectedEndOfQuote;
    }

    @Override
    public DynamicCommandExceptionType readerInvalidEscape() {
        return readerInvalidEscape;
    }

    @Override
    public DynamicCommandExceptionType readerInvalidBool() {
        return readerInvalidBool;
    }

    @Override
    public DynamicCommandExceptionType readerInvalidInt() {
        return readerInvalidInt;
    }

    @Override
    public SimpleCommandExceptionType readerExpectedInt() {
        return readerExpectedInt;
    }

    @Override
    public DynamicCommandExceptionType readerInvalidLong() {
        return readerInvalidLong;
    }

    @Override
    public SimpleCommandExceptionType readerExpectedLong() {
        return readerExpectedLong;
    }

    @Override
    public DynamicCommandExceptionType readerInvalidDouble() {
        return readerInvalidDouble;
    }

    @Override
    public SimpleCommandExceptionType readerExpectedDouble() {
        return readerExpectedDouble;
    }

    @Override
    public DynamicCommandExceptionType readerInvalidFloat() {
        return readerInvalidFloat;
    }

    @Override
    public SimpleCommandExceptionType readerExpectedFloat() {
        return readerExpectedFloat;
    }

    @Override
    public SimpleCommandExceptionType readerExpectedBool() {
        return readerExpectedBool;
    }

    @Override
    public DynamicCommandExceptionType readerExpectedSymbol() {
        return readerExpectedSymbol;
    }

    @Override
    public SimpleCommandExceptionType dispatcherUnknownCommand() {
        return dispatcherUnknownCommand;
    }

    @Override
    public SimpleCommandExceptionType dispatcherUnknownArgument() {
        return dispatcherUnknownArgument;
    }

    @Override
    public SimpleCommandExceptionType dispatcherExpectedArgumentSeparator() {
        return dispatcherExpectedArgumentSeparator;
    }

    @Override
    public DynamicCommandExceptionType dispatcherParseException() {
        return dispatcherParseException;
    }
}
