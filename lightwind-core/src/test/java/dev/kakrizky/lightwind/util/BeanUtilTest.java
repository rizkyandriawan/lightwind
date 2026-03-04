package dev.kakrizky.lightwind.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeanUtilTest {

    static class Source {
        private String name = "hello";
        private int age = 25;
        private String extra = "not in target";

        public String getName() { return name; }
        public int getAge() { return age; }
        public String getExtra() { return extra; }
    }

    static class Target {
        private String name;
        private int age;
        private String missing;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        public String getMissing() { return missing; }
        public void setMissing(String missing) { this.missing = missing; }
    }

    @Test
    void copyProperties_copiesMatchingFields() {
        Source src = new Source();
        Target tgt = new Target();
        BeanUtil.copyProperties(src, tgt);

        assertEquals("hello", tgt.getName());
        assertEquals(25, tgt.getAge());
    }

    @Test
    void copyProperties_ignoresSpecifiedFields() {
        Source src = new Source();
        Target tgt = new Target();
        BeanUtil.copyProperties(src, tgt, "name");

        assertNull(tgt.getName());
        assertEquals(25, tgt.getAge());
    }

    @Test
    void copyProperties_handlesNullSource() {
        Target tgt = new Target();
        assertDoesNotThrow(() -> BeanUtil.copyProperties(null, tgt));
    }

    @Test
    void copyProperties_handlesNullTarget() {
        Source src = new Source();
        assertDoesNotThrow(() -> BeanUtil.copyProperties(src, null));
    }

    static class Parent {
        private String parentField = "parent";
        public String getParentField() { return parentField; }
    }

    static class Child extends Parent {
        private String childField = "child";
        public String getChildField() { return childField; }
    }

    static class ChildTarget {
        private String parentField;
        private String childField;

        public String getParentField() { return parentField; }
        public void setParentField(String parentField) { this.parentField = parentField; }
        public String getChildField() { return childField; }
        public void setChildField(String childField) { this.childField = childField; }
    }

    @Test
    void copyProperties_worksWithInheritance() {
        Child src = new Child();
        ChildTarget tgt = new ChildTarget();
        BeanUtil.copyProperties(src, tgt);

        assertEquals("parent", tgt.getParentField());
        assertEquals("child", tgt.getChildField());
    }
}
