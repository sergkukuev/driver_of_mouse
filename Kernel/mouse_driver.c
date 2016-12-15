#include <linux/fs.h>
#include <asm/uaccess.h>
#include <linux/pci.h>
#include <linux/input.h>
#include <linux/platform_device.h>
#include <linux/module.h>
#include <linux/slab.h>
#include <linux/kernel.h>
#include <linux/usb.h>

#define LEFT_BUTTON_PRESSED 1
#define RIGHT_BUTTON_PRESSED 2
#define MOVE 3
#define DOUBLE_CLICK 4

#define KEY_WAS_PRESSED 8
#define KEY_WAS_RELEASED 0

#define ERROR_REGISTER_PLATFORM_DEVICE -1
#define ERROR_ALLOCATE_INPUT_DEVICE -2
#define ERROR_REGISTER_INPUT_DEVICE -3
#define ERROR_SYSFS_CREATE_GROUP -4

/* Представление устройства ввода */
struct input_dev *vms_input_dev; 

/* Структура представляющая устройство*/
static struct platform_device *vms_dev; 

/*  Метод доступный через sysfs для получения 
симулируемых координат драйвером виртуальной
мыши */
static ssize_t write_vms(struct device *dev, 
                        struct device_attribute *attr,
                        const char *buffer, size_t count)
{
    int x = 0, y = 0, z = 0, command_type = 0;

    sscanf(buffer, "%d%d%d%d", &command_type, &x, &y, &z);
    printk("coordinates = %d %d %d %d\n", command_type, x, y, z);

    input_report_rel(vms_input_dev, REL_X, -z);
    input_report_rel(vms_input_dev, REL_Y, -x);

    if (command_type == LEFT_BUTTON_PRESSED)
    {
        input_report_key(vms_input_dev, BTN_LEFT, KEY_WAS_PRESSED);
        input_report_key(vms_input_dev, BTN_LEFT, KEY_WAS_RELEASED);
    }
    else if (command_type == RIGHT_BUTTON_PRESSED)
    {
        input_report_key(vms_input_dev, BTN_RIGHT, KEY_WAS_PRESSED);
        input_report_key(vms_input_dev, BTN_RIGHT, KEY_WAS_RELEASED);
    }
    else if (command_type == DOUBLE_CLICK)
    {
        input_report_key(vms_input_dev, BTN_LEFT, KEY_WAS_PRESSED);   
        input_report_key(vms_input_dev, BTN_LEFT, KEY_WAS_RELEASED);    
        input_report_key(vms_input_dev, BTN_LEFT, KEY_WAS_PRESSED); 
        input_report_key(vms_input_dev, BTN_LEFT, KEY_WAS_RELEASED);    
    }
    input_sync(vms_input_dev);
    return count;
}

/* Привязка метода к файлы в sysfs */
DEVICE_ATTR(coordinates, 0644, NULL, write_vms);

/* Дексрипторы атрибутов */
static struct attribute *vms_attrs[] = 
{
    &dev_attr_coordinates.attr,
    NULL
};

/* Группы атрибутов */
static struct attribute_group vms_attr_group = 
{
    .attrs = vms_attrs,
};

/* Инициализация драйверов */
static int __init display_init(void)
{
    int command_result = 0;

    /* регистрация устройства*/
    vms_dev = platform_device_register_simple("vms", -1, NULL, 0);
    if (IS_ERR(vms_dev)) 
    {
        PTR_ERR(vms_dev);
        printk("vms_init: error\n");
        return ERROR_REGISTER_PLATFORM_DEVICE;
    }

    /* Создание файла в sysfs для чтения симулируемых координат */
    command_result = sysfs_create_group(&vms_dev->dev.kobj, &vms_attr_group);
    if (command_result < 0)
    {
        printk("Error sysfs_create_group\n");
        return ERROR_SYSFS_CREATE_GROUP;
    }

    /* Выделение памяти для структур устройства ввода */
    vms_input_dev = input_allocate_device();
    if (!vms_input_dev) 
    {
        printk("Bad input_alloc_device()\n");
        return ERROR_ALLOCATE_INPUT_DEVICE;
    }

    set_bit(EV_REL, vms_input_dev->evbit);
    set_bit(REL_X, vms_input_dev->relbit);
    set_bit(REL_Y, vms_input_dev->relbit);
    set_bit(EV_KEY, vms_input_dev->evbit);
    set_bit(BTN_LEFT, vms_input_dev->keybit);
    set_bit(BTN_RIGHT, vms_input_dev->keybit);


    vms_input_dev->evbit[0] = BIT_MASK(EV_KEY) | BIT_MASK(EV_REL);
    vms_input_dev->keybit[BIT_WORD(BTN_MOUSE)] = BIT_MASK(BTN_LEFT) |
        BIT_MASK(BTN_MIDDLE) | BIT_MASK(BTN_RIGHT);
    vms_input_dev->relbit[0] = BIT_MASK(REL_X) | BIT_MASK(REL_Y);

    vms_input_dev->name = "Virtual BT mouse";
    vms_input_dev->id.bustype = BUS_VIRTUAL;
    vms_input_dev->id.vendor  = 0x0000;
    vms_input_dev->id.product = 0x0000;
    vms_input_dev->id.version = 0x0000;
    
    /* регистрация в подсистеме ввода */
    command_result = input_register_device(vms_input_dev);
    if (command_result < 0)
    {
        printk("Error input_register_device\n");
        return ERROR_REGISTER_INPUT_DEVICE;
    }
    printk("Virtual BT Mouse Driver Initialized.\n");
    return 0;
}

/* Выход из драйвера */
static void vms_cleanup(void)
{
    /* Отмена регистрации в подсистеме ввода */
    input_unregister_device(vms_input_dev);
    input_free_device(vms_input_dev);

    /* Очистка файла в sysfs */
    sysfs_remove_group(&vms_dev->dev.kobj, &vms_attr_group);
    
    /* Отмена регистрации драйвера */
    platform_device_unregister(vms_dev);
}

module_init(display_init);
module_exit(vms_cleanup);

MODULE_LICENSE("GPL");
MODULE_AUTHOR("Kukuev Sergey");
MODULE_DESCRIPTION("Virtual BT mouse driver");