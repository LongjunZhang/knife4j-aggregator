/**
 * Schema Faker Service
 * 
 * 使用 json-schema-faker 库在前端本地生成测试数据
 * 替代原有的后端 AI 调用方式，实现毫秒级的参数生成体验
 */
import jsf from 'json-schema-faker';
import { faker } from '@faker-js/faker/locale/zh_CN';

// 配置 json-schema-faker
jsf.extend('faker', () => faker);
jsf.option({
  alwaysFakeOptionals: true,  // 总是生成可选字段
  minItems: 1,                // 数组最小元素数
  maxItems: 3,                // 数组最大元素数
  maxLength: 20,              // 字符串最大长度
  useDefaultValue: false,     // 不使用默认值，每次生成新数据
  useExamplesValue: false,    // 不使用示例值，每次生成新数据
  failOnInvalidTypes: false,  // 无效类型不报错
  failOnInvalidFormat: false  // 无效格式不报错
});

/**
 * 语义映射规则配置表
 * 根据字段名模式匹配对应的 Faker 方法，生成有语义的假数据
 */
const SEMANTIC_RULES = [
  // ========== 用户相关 ==========
  { pattern: /email/i, faker: 'internet.email' },
  { pattern: /phone|mobile|tel/i, faker: 'phone.number', args: ['1##########'] },
  { pattern: /username|loginname|login_name/i, faker: 'internet.userName' },
  { pattern: /nickname|nick_name|displayname|display_name/i, faker: 'person.firstName' },
  { pattern: /realname|real_name|fullname|full_name/i, faker: 'person.fullName' },
  { pattern: /password|pwd/i, faker: 'internet.password', args: [{ length: 12 }] },
  { pattern: /avatar|head_img|headimg/i, faker: 'image.avatar' },
  { pattern: /gender|sex/i, faker: 'person.sex' },
  { pattern: /age/i, faker: 'number.int', args: [{ min: 18, max: 60 }] },
  { pattern: /birthday|birth_date/i, faker: 'date.birthdate', args: [{ mode: 'age', min: 18, max: 60 }] },
  
  // ========== 商品/订单相关 ==========
  { pattern: /productname|product_name|goodsname|goods_name|itemname|item_name/i, faker: 'commerce.productName' },
  { pattern: /productid|product_id|goodsid|goods_id|sku/i, faker: 'string.alphanumeric', args: [{ length: 8, casing: 'upper' }] },
  { pattern: /unitprice|unit_price/i, faker: 'commerce.price', args: [{ min: 10, max: 9999, dec: 2 }] },
  { pattern: /price|amount|money|fee|cost/i, faker: 'commerce.price', args: [{ min: 1, max: 9999, dec: 2 }] },
  { pattern: /quantity|qty|count|num/i, faker: 'number.int', args: [{ min: 1, max: 100 }] },
  { pattern: /orderid|order_id|orderno|order_no|order_number/i, faker: 'string.alphanumeric', args: [{ length: 16, casing: 'upper' }] },
  { pattern: /category|categoryname|category_name/i, faker: 'commerce.department' },
  { pattern: /brand/i, faker: 'company.name' },
  { pattern: /stock/i, faker: 'number.int', args: [{ min: 0, max: 1000 }] },
  
  // ========== 地址相关 ==========
  { pattern: /address|addr/i, faker: 'location.streetAddress' },
  { pattern: /city/i, faker: 'location.city' },
  { pattern: /province|state/i, faker: 'location.state' },
  { pattern: /country/i, faker: 'location.country' },
  { pattern: /zipcode|zip_code|postcode|post_code/i, faker: 'location.zipCode' },
  { pattern: /latitude|lat/i, faker: 'location.latitude' },
  { pattern: /longitude|lng|lon/i, faker: 'location.longitude' },
  
  // ========== ID 相关 ==========
  { pattern: /uuid/i, faker: 'string.uuid' },
  { pattern: /userid|user_id/i, faker: 'number.int', args: [{ min: 10000, max: 99999 }] },
  { pattern: /addressid|address_id/i, faker: 'number.int', args: [{ min: 1000, max: 9999 }] },
  
  // ========== 时间相关 ==========
  { pattern: /createtime|create_time|created_at|createdat/i, faker: 'date.recent', args: [{ days: 30 }] },
  { pattern: /updatetime|update_time|updated_at|updatedat/i, faker: 'date.recent', args: [{ days: 7 }] },
  { pattern: /starttime|start_time|begintime|begin_time/i, faker: 'date.recent', args: [{ days: 7 }] },
  { pattern: /endtime|end_time/i, faker: 'date.soon', args: [{ days: 7 }] },
  { pattern: /date$/i, faker: 'date.recent' },
  { pattern: /time$/i, faker: 'date.recent' },
  
  // ========== 描述/备注 ==========
  { pattern: /remark|comment|note|memo/i, faker: 'lorem.sentence' },
  { pattern: /description|desc/i, faker: 'lorem.paragraph', args: [1] },
  { pattern: /title|subject/i, faker: 'lorem.words', args: [{ min: 2, max: 5 }] },
  { pattern: /content|body|text/i, faker: 'lorem.paragraphs', args: [{ min: 1, max: 3 }] },
  
  // ========== URL 相关 ==========
  { pattern: /url|link|website|homepage/i, faker: 'internet.url' },
  { pattern: /image|img|pic|photo|picture/i, faker: 'image.url' },
  
  // ========== 公司/组织相关 ==========
  { pattern: /company|companyname|company_name/i, faker: 'company.name' },
  { pattern: /department|dept/i, faker: 'commerce.department' },
  { pattern: /job|jobtitle|job_title|position/i, faker: 'person.jobTitle' },
  
  // ========== 其他常用 ==========
  { pattern: /ip|ipaddress|ip_address/i, faker: 'internet.ip' },
  { pattern: /mac|macaddress|mac_address/i, faker: 'internet.mac' },
  { pattern: /color/i, faker: 'color.human' },
  { pattern: /status/i, faker: 'number.int', args: [{ min: 0, max: 3 }] },
  { pattern: /type/i, faker: 'number.int', args: [{ min: 1, max: 5 }] },
  { pattern: /code/i, faker: 'string.alphanumeric', args: [{ length: 6, casing: 'upper' }] },
  { pattern: /token/i, faker: 'string.alphanumeric', args: [{ length: 32 }] },
  { pattern: /key/i, faker: 'string.alphanumeric', args: [{ length: 16 }] },
  { pattern: /version/i, faker: 'system.semver' },
  
  // ========== 分页相关 ==========
  { pattern: /pageno|page_no|pagenum|page_num|page$/i, faker: 'number.int', args: [{ min: 1, max: 1 }] },
  { pattern: /pagesize|page_size|size$/i, faker: 'number.int', args: [{ min: 10, max: 10 }] },
  { pattern: /total/i, faker: 'number.int', args: [{ min: 1, max: 1000 }] },
];

/**
 * 使用语义规则增强 JSON Schema
 * 根据字段名匹配语义规则，注入 x-faker 属性
 * 
 * @param {Object} schema - JSON Schema 对象
 * @param {string} fieldName - 当前字段名（用于匹配规则）
 * @returns {Object} 增强后的 schema
 */
function enrichSchemaWithSemantics(schema, fieldName = '') {
  if (!schema) return schema;
  
  // 深拷贝避免修改原始 schema
  const enrichedSchema = JSON.parse(JSON.stringify(schema));
  
  return enrichSchemaRecursive(enrichedSchema, fieldName);
}

/**
 * 递归增强 schema 的内部函数
 * 
 * @param {Object} schema - JSON Schema 对象
 * @param {string} fieldName - 当前字段名
 * @returns {Object} 增强后的 schema
 */
function enrichSchemaRecursive(schema, fieldName = '') {
  if (!schema) return schema;
  
  // 如果已经有 x-faker 属性或有枚举值，跳过
  if (schema['x-faker'] || (schema.enum && schema.enum.length > 0)) {
    // 递归处理嵌套属性
    processNestedProperties(schema);
    return schema;
  }
  
  // 根据字段名匹配语义规则
  if (fieldName) {
    for (const rule of SEMANTIC_RULES) {
      if (rule.pattern.test(fieldName)) {
        // 注入 x-faker 属性
        // json-schema-faker x-faker 格式:
        // - 无参数: "faker.method"
        // - 有参数: { "faker.method": args } 其中 args 可以是数组或对象
        if (rule.args !== undefined) {
          // 如果 args 是数组且只有一个元素，且该元素是对象，则直接使用该对象
          // 这样 { min: 10, max: 100 } 就不会变成 [{ min: 10, max: 100 }]
          const args = Array.isArray(rule.args) && rule.args.length === 1 && typeof rule.args[0] === 'object' && !Array.isArray(rule.args[0])
            ? rule.args[0]
            : rule.args;
          schema['x-faker'] = { [rule.faker]: args };
        } else {
          schema['x-faker'] = rule.faker;
        }
        break;
      }
    }
  }
  
  // 递归处理嵌套属性
  processNestedProperties(schema);
  
  return schema;
}

/**
 * 处理 schema 中的嵌套属性
 * 
 * @param {Object} schema - JSON Schema 对象
 */
function processNestedProperties(schema) {
  // 处理 object 的 properties
  if (schema.properties) {
    for (const [propName, propSchema] of Object.entries(schema.properties)) {
      schema.properties[propName] = enrichSchemaRecursive(propSchema, propName);
    }
  }
  
  // 处理数组的 items
  if (schema.items) {
    // items 可能是对象或数组
    if (Array.isArray(schema.items)) {
      schema.items = schema.items.map((item, index) => 
        enrichSchemaRecursive(item, `item${index}`)
      );
    } else {
      schema.items = enrichSchemaRecursive(schema.items, '');
    }
  }
  
  // 处理 additionalProperties
  if (schema.additionalProperties && typeof schema.additionalProperties === 'object') {
    schema.additionalProperties = enrichSchemaRecursive(schema.additionalProperties, '');
  }
  
  // 处理 allOf, anyOf, oneOf
  for (const key of ['allOf', 'anyOf', 'oneOf']) {
    if (Array.isArray(schema[key])) {
      schema[key] = schema[key].map(item => enrichSchemaRecursive(item, ''));
    }
  }
}

/**
 * 生成模拟数据的主入口
 * 
 * @param {Object} api - API 信息对象 (SwaggerBootstrapUiApiInfo)
 * @param {Object} swaggerInstance - Swagger 实例对象 (SwaggerBootstrapUiInstance)
 * @returns {Object} 生成的模拟数据
 */
export function generateMockData(api, swaggerInstance) {
  try {
    // 每次调用时重置随机种子，确保生成不同的数据
    jsf.option('random', () => Math.random());
    faker.seed(Date.now());
    
    const definitions = swaggerInstance.getOASDefinitions() || {};
    const oas2 = swaggerInstance.oas2();
    
    return {
      body: generateBodyData(api, definitions, oas2),
      queryParams: generateParamsByType(api.parameters, 'query', definitions, oas2),
      pathParams: generateParamsByType(api.parameters, 'path', definitions, oas2),
      headers: generateParamsByType(api.parameters, 'header', definitions, oas2)
    };
  } catch (error) {
    console.error('生成模拟数据失败:', error);
    return {
      body: null,
      queryParams: {},
      pathParams: {},
      headers: {}
    };
  }
}

/**
 * 生成请求体数据
 * 
 * @param {Object} api - API 信息对象
 * @param {Object} definitions - Schema 定义集合
 * @param {boolean} oas2 - 是否为 OpenAPI 2.0
 * @returns {Object|null} 生成的请求体数据
 */
function generateBodyData(api, definitions, oas2) {
  // 查找 body 类型的参数
  const bodyParams = (api.parameters || []).filter(p => p.in === 'body');
  
  if (bodyParams.length === 0) {
    // 如果没有找到 body 参数，尝试从 api.requestValue 解析现有示例
    if (api.requestValue) {
      try {
        const example = JSON.parse(api.requestValue);
        // 基于示例生成类似结构的数据
        return generateFromExample(example);
      } catch (e) {
        // requestValue 不是有效的 JSON
      }
    }
    return null;
  }
  
  const bodyParam = bodyParams[0];
  
  // 如果有 schemaValue，说明是引用类型
  if (bodyParam.schemaValue && definitions[bodyParam.schemaValue]) {
    const schema = buildJsonSchemaFromDefinition(
      bodyParam.schemaValue, 
      definitions, 
      oas2, 
      new Set()
    );
    return generateFromSchema(schema);
  }
  
  // 如果参数本身有 schema 定义
  if (bodyParam.schema) {
    const schema = paramToJsonSchema(bodyParam, definitions, oas2);
    return generateFromSchema(schema);
  }
  
  // 尝试从 api.requestValue 解析现有示例
  if (api.requestValue) {
    try {
      const example = JSON.parse(api.requestValue);
      // 基于示例生成类似结构的数据
      return generateFromExample(example);
    } catch (e) {
      // requestValue 不是有效的 JSON
    }
  }
  
  return null;
}

/**
 * 根据参数类型生成模拟数据
 * 
 * @param {Array} parameters - 参数列表
 * @param {string} paramIn - 参数位置 (query, path, header)
 * @param {Object} definitions - Schema 定义集合
 * @param {boolean} oas2 - 是否为 OpenAPI 2.0
 * @returns {Object} 参数名到值的映射
 */
function generateParamsByType(parameters, paramIn, definitions, oas2) {
  const result = {};
  
  if (!parameters || !Array.isArray(parameters)) {
    return result;
  }
  
  const filteredParams = parameters.filter(p => p.in === paramIn);
  
  for (const param of filteredParams) {
    try {
      const value = generateParamValue(param, definitions, oas2);
      if (value !== null && value !== undefined) {
        result[param.name] = value;
      }
    } catch (error) {
      console.warn(`生成参数 ${param.name} 失败:`, error);
    }
  }
  
  return result;
}

/**
 * 生成单个参数的值
 * 
 * @param {Object} param - 参数对象
 * @param {Object} definitions - Schema 定义集合
 * @param {boolean} oas2 - 是否为 OpenAPI 2.0
 * @returns {*} 生成的参数值
 */
function generateParamValue(param, definitions, oas2) {
  // 处理枚举类型 - 随机选择一个枚举值
  if (param.enum && Array.isArray(param.enum) && param.enum.length > 0) {
    return param.enum[Math.floor(Math.random() * param.enum.length)];
  }
  
  // 如果是引用类型
  if (param.schemaValue && definitions[param.schemaValue]) {
    const schema = buildJsonSchemaFromDefinition(
      param.schemaValue, 
      definitions, 
      oas2, 
      new Set()
    );
    return generateFromSchema(schema);
  }
  
  // 根据类型生成新的假数据
  return generateValueByType(param.type, param.name);
}

/**
 * 根据类型生成模拟值
 * 
 * @param {string} type - 参数类型
 * @param {string} name - 参数名称 (用于智能生成)
 * @returns {*} 生成的值
 */
function generateValueByType(type, name = '') {
  const lowerName = (name || '').toLowerCase();
  
  // 根据参数名称智能推断类型
  if (lowerName.includes('email')) {
    return faker.internet.email();
  }
  if (lowerName.includes('phone') || lowerName.includes('mobile') || lowerName.includes('tel')) {
    return faker.phone.number('1##########');
  }
  if (lowerName.includes('name') && lowerName.includes('user')) {
    return faker.person.fullName();
  }
  if (lowerName.includes('url') || lowerName.includes('link')) {
    return faker.internet.url();
  }
  if (lowerName.includes('address') || lowerName.includes('addr')) {
    return faker.location.streetAddress();
  }
  if (lowerName.includes('id') && !lowerName.includes('uuid')) {
    return faker.number.int({ min: 1, max: 9999 });
  }
  if (lowerName.includes('uuid')) {
    return faker.string.uuid();
  }
  if (lowerName.includes('date')) {
    return faker.date.recent().toISOString().split('T')[0];
  }
  if (lowerName.includes('time')) {
    return faker.date.recent().toISOString();
  }
  if (lowerName.includes('page') && (lowerName.includes('no') || lowerName.includes('num'))) {
    return 1;
  }
  if (lowerName.includes('size') || lowerName.includes('limit')) {
    return 10;
  }
  if (lowerName.includes('count') || lowerName.includes('total')) {
    return faker.number.int({ min: 0, max: 100 });
  }
  if (lowerName.includes('price') || lowerName.includes('amount') || lowerName.includes('money')) {
    return parseFloat(faker.commerce.price({ min: 1, max: 1000 }));
  }
  
  // 根据类型生成
  const normalizedType = (type || 'string').toLowerCase();
  
  switch (normalizedType) {
    case 'integer':
    case 'int':
    case 'int32':
    case 'int64':
    case 'long':
      return faker.number.int({ min: 1, max: 999 });
    
    case 'number':
    case 'float':
    case 'double':
      return parseFloat(faker.number.float({ min: 0, max: 100, fractionDigits: 2 }));
    
    case 'boolean':
    case 'bool':
      return faker.datatype.boolean();
    
    case 'array':
      return [];
    
    case 'object':
      return {};
    
    case 'string':
    default:
      return faker.lorem.word();
  }
}

/**
 * 将参数对象转换为 JSON Schema
 * 
 * @param {Object} param - 参数对象
 * @param {Object} definitions - Schema 定义集合
 * @param {boolean} oas2 - 是否为 OpenAPI 2.0
 * @returns {Object} JSON Schema
 */
function paramToJsonSchema(param, definitions, oas2) {
  const schema = {
    type: normalizeType(param.type)
  };
  
  // 保留枚举定义，faker 会随机选择
  if (param.enum) {
    schema.enum = param.enum;
  }
  
  // 注意：不再设置 schema.default，确保每次生成新的假数据
  
  if (param.description) {
    schema.description = param.description;
  }
  
  if (param.schemaValue && definitions[param.schemaValue]) {
    return buildJsonSchemaFromDefinition(param.schemaValue, definitions, oas2, new Set());
  }
  
  return schema;
}

/**
 * 从 definition 构建 JSON Schema
 * 
 * @param {string} refName - 引用名称
 * @param {Object} definitions - Schema 定义集合
 * @param {boolean} oas2 - 是否为 OpenAPI 2.0
 * @param {Set} visited - 已访问的引用（防止循环）
 * @returns {Object} JSON Schema
 */
function buildJsonSchemaFromDefinition(refName, definitions, oas2, visited) {
  // 防止循环引用
  if (visited.has(refName)) {
    return { type: 'object', properties: {} };
  }
  visited.add(refName);
  
  const def = definitions[refName];
  if (!def) {
    return { type: 'object', properties: {} };
  }
  
  const schema = {
    type: 'object',
    properties: {}
  };
  
  if (def.required) {
    schema.required = def.required;
  }
  
  // 处理 properties
  if (def.properties) {
    for (const [propName, propDef] of Object.entries(def.properties)) {
      schema.properties[propName] = convertPropertyToSchema(propDef, definitions, oas2, new Set(visited));
    }
  }
  
  // 处理 additionalProperties (Map 类型)
  if (def.additionalProperties) {
    schema.additionalProperties = convertPropertyToSchema(
      def.additionalProperties, 
      definitions, 
      oas2, 
      new Set(visited)
    );
  }
  
  return schema;
}

/**
 * 将属性定义转换为 JSON Schema
 * 
 * @param {Object} propDef - 属性定义
 * @param {Object} definitions - Schema 定义集合
 * @param {boolean} oas2 - 是否为 OpenAPI 2.0
 * @param {Set} visited - 已访问的引用
 * @returns {Object} JSON Schema
 */
function convertPropertyToSchema(propDef, definitions, oas2, visited) {
  // 处理 $ref 引用
  const refType = extractRefType(propDef, oas2);
  if (refType && definitions[refType]) {
    return buildJsonSchemaFromDefinition(refType, definitions, oas2, visited);
  }
  
  const schema = {
    type: normalizeType(propDef.type)
  };
  
  // 处理枚举 - 保留枚举定义，faker 会随机选择
  if (propDef.enum) {
    schema.enum = propDef.enum;
  }
  
  // 注意：不再设置 schema.default，确保每次生成新的假数据
  
  // 处理格式
  if (propDef.format) {
    schema.format = propDef.format;
  }
  
  // 处理数组类型
  if (propDef.type === 'array' && propDef.items) {
    const itemRefType = extractRefType(propDef.items, oas2);
    if (itemRefType && definitions[itemRefType]) {
      schema.items = buildJsonSchemaFromDefinition(itemRefType, definitions, oas2, visited);
    } else {
      schema.items = convertPropertyToSchema(propDef.items, definitions, oas2, visited);
    }
  }
  
  // 处理嵌套对象
  if (propDef.type === 'object' && propDef.properties) {
    schema.properties = {};
    for (const [name, prop] of Object.entries(propDef.properties)) {
      schema.properties[name] = convertPropertyToSchema(prop, definitions, oas2, visited);
    }
  }
  
  return schema;
}

/**
 * 从属性定义中提取 $ref 类型名称
 * 
 * @param {Object} propDef - 属性定义
 * @param {boolean} oas2 - 是否为 OpenAPI 2.0
 * @returns {string|null} 类型名称
 */
function extractRefType(propDef, oas2) {
  if (!propDef) return null;
  
  let ref = propDef['$ref'];
  
  if (!ref && propDef.originalRef) {
    ref = propDef.originalRef;
  }
  
  if (!ref) return null;
  
  // 处理 #/definitions/TypeName (OAS2) 或 #/components/schemas/TypeName (OAS3)
  if (oas2) {
    const match = ref.match(/#\/definitions\/(.+)/);
    return match ? match[1] : null;
  } else {
    const match = ref.match(/#\/components\/schemas\/(.+)/);
    return match ? match[1] : null;
  }
}

/**
 * 标准化类型名称
 * 
 * @param {string} type - 原始类型
 * @returns {string} 标准化后的类型
 */
function normalizeType(type) {
  if (!type) return 'string';
  
  const normalized = type.toLowerCase();
  
  switch (normalized) {
    case 'integer':
    case 'int':
    case 'int32':
    case 'int64':
    case 'long':
      return 'integer';
    
    case 'number':
    case 'float':
    case 'double':
      return 'number';
    
    case 'boolean':
    case 'bool':
      return 'boolean';
    
    case 'array':
      return 'array';
    
    case 'object':
      return 'object';
    
    default:
      return 'string';
  }
}

/**
 * 使用 json-schema-faker 从 schema 生成数据
 * 在生成前会注入语义化的 x-faker 属性
 * 
 * @param {Object} schema - JSON Schema
 * @returns {*} 生成的数据
 */
function generateFromSchema(schema) {
  try {
    // 使用语义规则增强 schema，注入 x-faker 属性
    const enrichedSchema = enrichSchemaWithSemantics(schema);
    return jsf.generate(enrichedSchema);
  } catch (error) {
    console.warn('json-schema-faker 生成失败:', error);
    // 降级处理
    if (schema.type === 'object') {
      return generateFromSchemaFallback(schema);
    }
    return null;
  }
}

/**
 * 降级方案：手动生成模拟数据
 * 
 * @param {Object} schema - JSON Schema
 * @returns {Object} 生成的数据
 */
function generateFromSchemaFallback(schema) {
  if (!schema.properties) {
    return {};
  }
  
  const result = {};
  
  for (const [propName, propSchema] of Object.entries(schema.properties)) {
    if (propSchema.enum && propSchema.enum.length > 0) {
      // 随机选择一个枚举值
      result[propName] = propSchema.enum[Math.floor(Math.random() * propSchema.enum.length)];
    } else {
      // 总是生成新的假数据
      result[propName] = generateValueByType(propSchema.type, propName);
    }
  }
  
  return result;
}

/**
 * 基于示例数据生成类似结构的新数据
 * 每次调用都会生成完全新的假数据
 * 
 * @param {Object} example - 示例数据
 * @returns {Object} 生成的数据
 */
function generateFromExample(example) {
  if (example === null || example === undefined) {
    return null;
  }
  
  if (Array.isArray(example)) {
    if (example.length === 0) {
      return [];
    }
    // 基于第一个元素生成新的假数据
    return [generateFromExample(example[0])];
  }
  
  if (typeof example === 'object') {
    const result = {};
    for (const [key, value] of Object.entries(example)) {
      if (typeof value === 'object' && value !== null) {
        result[key] = generateFromExample(value);
      } else {
        // 总是基于类型和字段名生成新的假数据
        result[key] = generateValueByType(typeof value, key);
      }
    }
    return result;
  }
  
  // 对于原始类型，也生成新的假数据
  return generateValueByType(typeof example, '');
}

export default {
  generateMockData
};

