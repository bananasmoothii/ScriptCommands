package fr.bananasmoothii.scriptcommands.core.configs_storage;

public interface ScriptValueContainer {
    ScriptValueContainer recursiveClone();

    Object toNormalClasses();
}
